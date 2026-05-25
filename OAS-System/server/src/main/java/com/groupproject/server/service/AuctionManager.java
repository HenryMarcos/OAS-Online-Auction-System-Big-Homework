package com.groupproject.server.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.dao.BidDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.event.AuctionCancelledEvent;
import com.groupproject.shared.network.event.AuctionEndedEvent;
import com.groupproject.shared.network.event.AuctionFinishedEvent;
import com.groupproject.shared.network.event.AuctionStartedEvent;
import com.groupproject.shared.network.event.SystemNotificationEvent;

public class AuctionManager {

    // Tìm nhanh các phiên đấu giá còn đang hoạt động trên RAM
    private final ConcurrentHashMap<Integer, Auction> activeAuctions = new ConcurrentHashMap<>();

    // Xử lý tất cả phần thời gian đấu giá của các phiên đấu giá
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    // =========================================================================
    // 1. NHÓM KHỞI TẠO & SINGLETON (INITIALIZATION & SETUP)
    // =========================================================================

    // Constructor private để ngăn chặn việc khởi tạo từ bên ngoài
    private AuctionManager() {
        // Khi khởi tạo, load tất cả các phiên đấu giá đang hoạt động từ database vào bộ nhớ
        loadActiveAuctionsFromDatabase();
        
        // Đăng ký tiến trình dọn dẹp hệ thống chạy ngầm:
        scheduler.scheduleAtFixedRate(
            this::runHousekeepingTask, // Công việc dọn dẹp tự động
            1,                         // Đợi 1 phút sau khi bật Server mới chạy lần đầu
            5,                         // Sau đó cứ 5 phút thì lặp lại một lần
            TimeUnit.MINUTES           // Đơn vị thời gian
        );
    }

    // Static inner class chứa instance duy nhất (The Bill Pugh concept)
    private static class AuctionManagerHelper {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    // Phương thức lấy instance hoàn toàn không cần 'synchronized'
    public static AuctionManager getInstance() {
        return AuctionManagerHelper.INSTANCE;
    }

    /**
     * TỔNG TỔNG VỆ SINH HỆ THỐNG (Housekeeping)
     * Bao phủ 3 trường hợp: Quá hạn WAITING, Lỡ giờ SCHEDULED, Lỡ giờ ACTIVED
     */
    private void runHousekeepingTask() {
        ServerLogger.info("--- [Housekeeping] Starting system-wide cleanup scan... ---");
        
        try {
            LocalDateTime now = LocalDateTime.now();

            // TRƯỜNG HỢP 1: Các phiên WAITING đã "ngâm" quá lâu
            List<Integer> expiredWaitingIds = AuctionDAO.getExpiredWaitingAuctions(now.minusMinutes(15)); // Giờ bắt đầu dự kiến đã quá 15 phút
            for (int id : expiredWaitingIds) {
                ServerLogger.info("[Housekeeping] Cancelling expired WAITING auction: " + id);
                forceCancelWaitingAuction(id); 
            }

            // TRƯỜNG HỢP 2: Các phiên SCHEDULED bị kẹt (lẽ ra phải tự chạy nhưng chưa chạy do sập Server)
            List<Integer> missedScheduledIds = AuctionDAO.getMissedScheduledAuctions(now);
            for (int id : missedScheduledIds) {
                ServerLogger.info("[Housekeeping] Auto-starting missed SCHEDULED auction: " + id);
                startAuction(id); 
            }

            // TRƯỜNG HỢP 3: Các phiên ACTIVED đã quá giờ kết thúc nhưng vẫn đang treo do sập Server lúc đang chạy
            List<Integer> missedActiveIds = AuctionDAO.getExpiredActiveAuctions(now);
            for (int id : missedActiveIds) {
                ServerLogger.info("[Housekeeping] Finalizing missed ACTIVED auction: " + id);
                finishAuction(id); 
            }

            ServerLogger.info("--- [Housekeeping] Cleanup scan completed successfully. ---");
        } catch (Exception e) {
            ServerLogger.error("CRITICAL: Housekeeping Task failed! Reason: " + e.getMessage());
        }
    }

    private void loadActiveAuctionsFromDatabase() {
        // Chỉ lấy những cái thực sự cần thiết từ Database khi khởi động
        for (Auction auction : AuctionDAO.getActiveAuctions()) {
            registerAuction(auction);
        }
    }

    // Thêm một phiên đấu giá mới vào hệ thống (dùng khi tạo mới hoặc load từ DB)
    public void registerAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);

        if (auction.getStatus() == AuctionStatus.ACTIVED) {
            // Đếm ngược đến endTime chuyển từ ACTIVED -> FINISHED
            long delayEnd = Duration.between(LocalDateTime.now(), auction.getEndTime()).toSeconds();
            scheduler.schedule(() -> finishAuction(auction.getId()), Math.max(0, delayEnd), TimeUnit.SECONDS);
            
        } else if (auction.getStatus() == AuctionStatus.SCHEDULED) {
            // Đếm ngược đến startTime chuyển từ SCHEDULED -> ACTIVED
            long delayStart = Duration.between(LocalDateTime.now(), auction.getStartTime()).toSeconds();
            scheduler.schedule(() -> startAuction(auction.getId()), Math.max(0, delayStart), TimeUnit.SECONDS);
            
            ServerLogger.info("Auction " + auction.getId() + " is SCHEDULED. Start timer set in " + delayStart + "s.");
        }
    }

    // =========================================================================
    // 2. NHÓM TÁC VỤ THỦ CÔNG (MANUAL ACTIONS - Do người dùng/Admin tác động)
    // =========================================================================

    /**
     * Dùng khi người bán tự bấm nút "Start Now" (Chuyển WAITING -> ACTIVED)
     */
    public void activateWaitingAuction(Auction updatedAuction) {
        activeAuctions.put(updatedAuction.getId(), updatedAuction);

        long delayEnd = Duration.between(LocalDateTime.now(), updatedAuction.getEndTime()).toSeconds();
        if (delayEnd < 0) { delayEnd = 0; }

        scheduler.schedule(() -> finishAuction(updatedAuction.getId()), delayEnd, TimeUnit.SECONDS);

        ServerLogger.info("Auction " + updatedAuction.getId() + " manually ACTIVATED. End timer set in " + delayEnd + "s.");
        
        // =====================================================================
        // [RÁP EVENT] KÍCH HOẠT THỦ CÔNG PHIÊN ĐẤU GIÁ
        // =====================================================================
        // LUỒNG 1: Bắn cho toàn hệ thống (Lobby) biết để hiển thị thẻ đấu giá đang hoạt động
        AuctionStartedEvent event = new AuctionStartedEvent(updatedAuction.getId());
        ClientManager.getInstance().broadcastSystemEvent(event);

        // LUỒNG 3: Báo cho Admin lưu log hiển thị hệ thống
        SystemNotificationEvent adminLog = new SystemNotificationEvent(
            "Người bán đã kích hoạt thủ công phiên đấu giá ID: " + updatedAuction.getId(), "Hệ Thống"
        );
        ClientManager.getInstance().broadcastToAdmins(adminLog);
        // =====================================================================
    }

    /**
     * Dùng khi hệ thống ép kết thúc một phiên WAITING do bấm Start quá trễ
     */
    public void forceCancelWaitingAuction(int auctionId) {
        // 1. Gỡ khỏi RAM quản lý
        Auction auction = activeAuctions.remove(auctionId);
        
        if (auction != null) {
            // 2. Cập nhật trạng thái xuống Database là CANCELLED
            boolean isUpdated = AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.CANCELLED);
            
            if (isUpdated) {
                auction.setStatus(AuctionStatus.CANCELLED);
                ServerLogger.info("Auction " + auctionId + " forced to CANCELLED because it expired in WAITING state.");
                
                // =====================================================================
                // [RÁP EVENT] ÉP HỦY PHIÊN ĐẦU GIÁ CHỜ QUÁ HẠN
                // =====================================================================
                AuctionCancelledEvent event = new AuctionCancelledEvent(auctionId, "Phiên đấu giá bị hệ thống hủy do không được bắt đầu đúng hạn.");
                
                // LUỒNG 1: Báo cho toàn hệ thống gỡ thẻ này khỏi trang chủ UI
                ClientManager.getInstance().broadcastSystemEvent(event);

                // LUỒNG 2: Phát thông báo cho những người đang ở trong phòng (nếu có)
                ClientManager.getInstance().broadcastEventToAuction(auctionId, event);
                
                // LUỒNG 3: Ghi log cho Admin
                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Hệ thống ép hủy phiên WAITING quá hạn (ID: " + auctionId + ")", "Hệ Thống"
                );
                ClientManager.getInstance().broadcastToAdmins(adminLog);

                // 4. Giải phóng phòng đấu giá
                ClientManager.getInstance().removeAuctionRoom(auctionId);
                // =====================================================================
            }
        }
    }

    /**
     * Dùng khi người bán hoặc Admin HỦY phiên đấu giá đang diễn ra
     */
    public void cancelAuction(int auctionId) {
        Auction auction = activeAuctions.remove(auctionId);
        
        if (auction != null) {
            boolean isUpdated = AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.CANCELLED);
            
            if (isUpdated) {
                auction.setStatus(AuctionStatus.CANCELLED);
                ServerLogger.info("Auction " + auctionId + " manually CANCELLED and removed from active RAM memory.");
                
                // =====================================================================
                // [RÁP EVENT] NGƯỜI BÁN HOẶC ADMIN HỦY PHIÊN ĐẦU GIÁ THỦ CÔNG
                // =====================================================================
                AuctionCancelledEvent cancelledEvent = new AuctionCancelledEvent(auctionId, "Phiên đấu giá đã bị hủy bởi người bán hoặc quản trị viên.");
                
                // LUỒNG 1: Báo toàn hệ thống dọn dẹp thẻ hiển thị trên trang chủ
                ClientManager.getInstance().broadcastSystemEvent(cancelledEvent);

                // LUỒNG 2: Phát thông báo trực tiếp cho những người đang trong phòng
                ClientManager.getInstance().broadcastEventToAuction(auctionId, cancelledEvent);
                
                // LUỒNG 3: Gửi Log hệ thống báo cho Admin
                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Phiên đấu giá ID " + auctionId + " bị hủy thủ công bởi người bán hoặc Admin.", "Hệ Thống"
                );
                ClientManager.getInstance().broadcastToAdmins(adminLog);

                // Giải phóng phòng đấu giá khỏi RAM
                ClientManager.getInstance().removeAuctionRoom(auctionId);
                // =====================================================================
            }
        }
    }

    // =========================================================================
    // 3. NHÓM VÒNG ĐỜI TỰ ĐỘNG (AUTO LIFECYCLE - Do Scheduler tự kích hoạt)
    // =========================================================================

    /**
     * TỰ ĐỘNG mở phiên đấu giá đã lên lịch (Chuyển SCHEDULED -> ACTIVED)
     */
    private void startAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        
        if (auction != null && auction.getStatus() == AuctionStatus.SCHEDULED) {
            LocalDateTime now = LocalDateTime.now();
            
            boolean isUpdatedInDb = AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.ACTIVED, now);
            
            if (isUpdatedInDb) {
                auction.setStatus(AuctionStatus.ACTIVED);
                auction.setStartTime(now); 
                
                long delayEnd = Duration.between(now, auction.getEndTime()).toSeconds();
                if (delayEnd < 0) { delayEnd = 0; }
                
                scheduler.schedule(() -> finishAuction(auctionId), delayEnd, TimeUnit.SECONDS);
                
                ServerLogger.info("Auction " + auctionId + " auto-started from SCHEDULED. End timer set in " + delayEnd + "s.");
                
                // =====================================================================
                // [RÁP EVENT] TỰ ĐỘNG MỞ PHIÊN ĐẤU GIÁ ĐÃ LÊN LỊCH
                // =====================================================================
                AuctionStartedEvent event = new AuctionStartedEvent(auctionId);
                
                // LUỒNG 1: Phát toàn hệ thống để trang chủ Client kích hoạt đổi màu trạng thái
                ClientManager.getInstance().broadcastSystemEvent(event);

                // LUỒNG 2: Báo cho những người đã trực trong phòng từ trước
                ClientManager.getInstance().broadcastEventToAuction(auctionId, event);

                // LUỒNG 3: Báo cho Admin lưu log sự kiện
                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Hệ thống tự động kích hoạt phiên đấu giá lên lịch thành công (ID: " + auctionId + ")", "Hệ Thống"
                );
                ClientManager.getInstance().broadcastToAdmins(adminLog);
                // =====================================================================
            } else {
                ServerLogger.error("Failed to auto-start SCHEDULED auction " + auctionId + " in Database.");
            }
        }
    }

    /**
     * TỰ ĐỘNG chốt sổ khi hết giờ đếm ngược (Chuyển ACTIVED -> FINISHED)
     * Đây là bước khóa Bid, ngưng nhận giá, chuẩn bị đối soát giao dịch
     */
    private void finishAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVED) return;

        if (AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.FINISHED)) {
            auction.setStatus(AuctionStatus.FINISHED);

            ServerLogger.info("Auction " + auctionId + " time count end. Status changed to FINISHED.");

            // =====================================================================
            // [RÁP EVENT] PHIÊN HẾT GIỜ (CHỜ XỬ LÝ THANH TOÁN)
            // =====================================================================
            AuctionFinishedEvent finishedEvent = new AuctionFinishedEvent(auctionId);

            // LUỒNG 2: Báo khẩn cấp trong phòng để Client đóng băng nút Bid và hiển thị "Đang xử lý giao dịch..."
            ClientManager.getInstance().broadcastEventToAuction(auctionId, finishedEvent);

            // LUỒNG 1: Báo toàn Server để dẹp thẻ này khỏi Main UI (Lobby) người dùng thường
            ClientManager.getInstance().broadcastSystemEvent(finishedEvent);

            // LUỒNG 3: Báo Admin ghi nhận log hệ thống chuẩn bị đối soát
            SystemNotificationEvent adminLog = new SystemNotificationEvent(
                "Phiên ID " + auctionId + " đã hết giờ. Bắt đầu đối soát giao dịch ví...", "Hệ Thống"
            );
            ClientManager.getInstance().broadcastToAdmins(adminLog);
            // =====================================================================

            // BẮT ĐẦU QUY TRÌNH THANH TOÁN
            double finalPrice = auction.getCurrentBid();
            Integer winnerId = auction.getHighestBidderId();
            int sellerId = auction.getSellerId();

            // Nếu không có ai đặt giá hoặc không có Winner
            if (finalPrice <= 0 || winnerId == null) {
                ServerLogger.info("No bids for auction " + auctionId + ". Automatically cancelling...");
                cancelFinishedAuction(auctionId);
                return;
            }

            // Giao dịch chuyển tiền trực tiếp trong Database
            boolean transferOk = BidDAO.executeDirectTransfer(winnerId, sellerId, finalPrice);

            if (transferOk) {
                ServerLogger.info("Transfer completed for auction " + auctionId);
                endAuction(auctionId); // Xác nhận thành công hoàn toàn
            } else {
                ServerLogger.error("Critical: Transfer failed for auction " + auctionId + ". Buyer insufficient balance.");
                cancelFinishedAuction(auctionId); // Giao dịch lỗi/bùng tiền -> Hủy phiên
            }
        }
    }

    /**
     * TỰ ĐỘNG đóng hoàn toàn phiên đấu giá thành công (Chuyển FINISHED -> ENDED)
     */
    private void endAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        
        if (auction != null && auction.getStatus() == AuctionStatus.FINISHED) {
            ServerLogger.info("Post-processing complete. Closing auction " + auctionId + " (ENDED).");
            
            boolean isEndedInDb = AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.ENDED);
            
            if (isEndedInDb) {
                int winnerId = auction.getHighestBidderId() != null ? auction.getHighestBidderId() : 0;
                double finalPrice = auction.getCurrentBid();

                auction.setStatus(AuctionStatus.ENDED);
                activeAuctions.remove(auctionId); // Xóa khỏi RAM quản lý
                
                ServerLogger.info("Auction " + auctionId + " fully ENDED and cleared from RAM.");

                // =====================================================================
                // [RÁP EVENT] GIAO DỊCH THÀNH CÔNG - KẾT THÚC PHIÊN ĐẤU GIÁ
                // =====================================================================
                AuctionEndedEvent endedEvent = new AuctionEndedEvent(auctionId, winnerId, finalPrice);

                // LUỒNG 2: Công bố người thắng cuộc và số tiền chốt cho những người trong phòng
                ClientManager.getInstance().broadcastEventToAuction(auctionId, endedEvent);

                // LUỒNG 1: Báo toàn hệ thống dọn dẹp thẻ này khỏi trang chủ UI người dùng nếu còn sót
                ClientManager.getInstance().broadcastSystemEvent(endedEvent);

                // LUỒNG 3: Ghi nhận Log hoàn tất giao dịch cho Admin
                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Phiên ID " + auctionId + " thành công mỹ mãn. Winner ID: " + winnerId + " chốt giá " + finalPrice, "Hệ Thống"
                );
                ClientManager.getInstance().broadcastToAdmins(adminLog);

                // Giải tán phòng đấu giá hoàn toàn
                ClientManager.getInstance().removeAuctionRoom(auctionId);
                // =====================================================================
            }
        }
    }

    /**
     * TỰ ĐỘNG hủy phiên đấu giá do không có người mua hoặc lỗi giao dịch (Chuyển FINISHED -> CANCELLED)
     */
    private void cancelFinishedAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        
        if (auction != null && auction.getStatus() == AuctionStatus.FINISHED) {
            boolean isCancelledInDb = AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.CANCELLED);
            
            if (isCancelledInDb) {
                auction.setStatus(AuctionStatus.CANCELLED);
                activeAuctions.remove(auctionId); // Xóa khỏi RAM quản lý
                ServerLogger.info("Auction " + auctionId + " fully CANCELLED and cleared from RAM.");

                // =====================================================================
                // [RÁP EVENT] GIAO DỊCH THẤT BẠI - HỦY PHIÊN ĐÃ KẾT THÚC
                // =====================================================================
                AuctionCancelledEvent cancelledEvent = new AuctionCancelledEvent(auctionId, "Không có người đặt giá hoặc người mua không đủ số dư thanh toán.");

                // LUỒNG 2: Báo lỗi thanh toán/không ai Bid cho những người trong phòng biết
                ClientManager.getInstance().broadcastEventToAuction(auctionId, cancelledEvent);

                // LUỒNG 1: Báo toàn hệ thống gỡ hoàn toàn thẻ đấu giá này
                ClientManager.getInstance().broadcastSystemEvent(cancelledEvent);

                // LUỒNG 3: Báo cho Admin log hệ thống
                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Phiên ID " + auctionId + " bị hệ thống hủy. Lý do: Không ai Bid hoặc ví của người thắng không đủ tiền.", "Hệ Thống"
                );
                ClientManager.getInstance().broadcastToAdmins(adminLog);

                // Giải tán phòng đấu giá
                ClientManager.getInstance().removeAuctionRoom(auctionId);
                // =====================================================================
            }
        }
    }

    // =========================================================================
    // 4. NHÓM XỬ LÝ ĐẤU GIÁ (BID HANDLING)
    // =========================================================================

    /**
     * Cập nhật thông tin giá trị phiên đấu giá trên RAM sau khi Database đã ghi nhận Bid thành công
     */
    public synchronized void updateRamAfterBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidderId);
        }
    }
}