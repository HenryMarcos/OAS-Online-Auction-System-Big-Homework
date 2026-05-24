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
import com.groupproject.shared.network.event.AuctionStartedEvent;
import com.groupproject.shared.network.event.AuctionFinishedEvent;
import com.groupproject.shared.network.event.AuctionEndedEvent;
import com.groupproject.shared.network.event.AuctionCancelledEvent;

public class AuctionManager {

    // Tìm nhanh các phiên đấu giá còn đang hoạt động
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
        // Đăng ký hàm chạy ngầm ở đây:
        scheduler.scheduleAtFixedRate(
            this::runHousekeepingTask, // Công việc cần làm
            1,           // Đợi 1 phút sau khi bật Server mới chạy lần đầu
            5,                 // Sau đó cứ 5 phút thì lặp lại một lần
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

            // TRƯỜNG HỢP 1: Các phiên WAITING đã "ngâm" quá lâu (ví dụ quá 15 phút so với giờ bắt đầu dự kiến)
            // Logic: Hủy bỏ vì người bán không chịu kích hoạt đúng hạn.
            List<Integer> expiredWaitingIds = AuctionDAO.getExpiredWaitingAuctions(now.minusMinutes(1));
            for (int id : expiredWaitingIds) {
                ServerLogger.info("[Housekeeping] Cancelling expired WAITING auction: " + id);
                forceCancelWaitingAuction(id); 
                // Hàm này bên dưới đã bao gồm: Cập nhật DB -> Broadcast -> Xóa RAM & Room.
            }

            // TRƯỜNG HỢP 2: Các phiên SCHEDULED bị kẹt (lẽ ra phải tự chạy nhưng chưa chạy)
            // Logic: Thường xảy ra khi server sập đúng lúc giờ Start. Ta cần kích hoạt ngay.
            List<Integer> missedScheduledIds = AuctionDAO.getMissedScheduledAuctions(now);
            for (int id : missedScheduledIds) {
                ServerLogger.info("[Housekeeping] Auto-starting missed SCHEDULED auction: " + id);
                startAuction(id); 
                // Hàm này sẽ: Cập nhật DB -> Lên lịch kết thúc -> Broadcast Started.
            }

            // TRƯỜNG HỢP 3: Các phiên ACTIVED đã quá giờ kết thúc nhưng vẫn đang treo
            // Logic: Server sập lúc phiên đang chạy, khi bật lại đã quá giờ kết thúc.
            // Cần chốt sổ ngay để trả hàng/thu tiền.
            List<Integer> missedActiveIds = AuctionDAO.getExpiredActiveAuctions(now);
            for (int id : missedActiveIds) {
                ServerLogger.info("[Housekeeping] Finalizing missed ACTIVED auction: " + id);
                finishAuction(id); 
                // Hàm này sẽ: Chốt sổ -> Giao dịch tiền -> Broadcast Ended -> Xóa RAM & Room.
            }

            ServerLogger.info("--- [Housekeeping] Cleanup scan completed successfully. ---");
        } catch (Exception e) {
            ServerLogger.error("CRITICAL: Housekeeping Task failed! Reason: " + e.getMessage());
        }
    }

    private void loadActiveAuctionsFromDatabase() {
        // Chỉ lấy những cái thực sự cần thiết từ Database
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
        
        AuctionStartedEvent event = new AuctionStartedEvent(updatedAuction.getId());
        ClientManager.getInstance().broadcastEventToAuction(updatedAuction.getId(), event);
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
                
                // 3. Phát thông báo cho những người đang ở trong phòng (nếu có)
                AuctionCancelledEvent event = new AuctionCancelledEvent(auctionId, "Phiên đấu giá bị hủy do không được bắt đầu đúng hạn.");
                ClientManager.getInstance().broadcastEventToAuction(auctionId, event);
                
                // 4. Giải phóng phòng đấu giá
                ClientManager.getInstance().removeAuctionRoom(auctionId);
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
                
                // Phát thông báo & Giải phóng phòng
                AuctionCancelledEvent cancelledEvent = new AuctionCancelledEvent(auctionId, "Phiên đấu giá đã bị hủy bởi người bán hoặc quản trị viên.");
                ClientManager.getInstance().broadcastEventToAuction(auctionId, cancelledEvent);
                ClientManager.getInstance().removeAuctionRoom(auctionId);
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
                
                AuctionStartedEvent event = new AuctionStartedEvent(auctionId);
                ClientManager.getInstance().broadcastEventToAuction(auctionId, event);
            } else {
                ServerLogger.error("Failed to auto-start SCHEDULED auction " + auctionId + " in Database.");
            }
        }
    }

    /**
     * TỰ ĐỘNG chốt sổ khi hết giờ đếm ngược (Chuyển ACTIVED -> FINISHED)
     */
    private void finishAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVED) return;

        if (AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.FINISHED)) {
            auction.setStatus(AuctionStatus.FINISHED);

            // Thông báo cục bộ: Đã hết giờ đặt giá
            AuctionFinishedEvent finishedEvent = new AuctionFinishedEvent(auctionId);
            ClientManager.getInstance().broadcastEventToAuction(auctionId, finishedEvent);

            double finalPrice = auction.getCurrentBid();
            Integer winnerId = auction.getHighestBidderId();
            int sellerId = auction.getSellerId();

            if (finalPrice <= 0 || winnerId == null) {
                ServerLogger.info("No bids for auction " + auctionId + ". Cancelling...");
                cancelFinishedAuction(auctionId);
                return;
            }

            // Giao dịch tiền tệ qua DAO
            boolean transferOk = BidDAO.executeDirectTransfer(winnerId, sellerId, finalPrice);

            if (transferOk) {
                ServerLogger.info("Transfer completed for auction " + auctionId);
                endAuction(auctionId);
            } else {
                ServerLogger.error("Critical: Transfer failed for auction " + auctionId);
                cancelFinishedAuction(auctionId);
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
                activeAuctions.remove(auctionId);
                
                ServerLogger.info("Auction " + auctionId + " fully ENDED and cleared from RAM.");

                // Công bố người chiến thắng & Giải tán phòng
                AuctionEndedEvent endedEvent = new AuctionEndedEvent(auctionId, winnerId, finalPrice);
                ClientManager.getInstance().broadcastEventToAuction(auctionId, endedEvent);
                ClientManager.getInstance().removeAuctionRoom(auctionId);
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
                activeAuctions.remove(auctionId);
                ServerLogger.info("Auction " + auctionId + " fully CANCELLED and cleared from RAM.");

                // Thông báo hủy & Giải tán phòng
                AuctionCancelledEvent cancelledEvent = new AuctionCancelledEvent(auctionId, "Không có người đặt giá hoặc giao dịch lỗi.");
                ClientManager.getInstance().broadcastEventToAuction(auctionId, cancelledEvent);
                ClientManager.getInstance().removeAuctionRoom(auctionId);
            }
        }
    }

    // =========================================================================
    // 4. NHÓM XỬ LÝ ĐẤU GIÁ (BID HANDLING)
    // =========================================================================

    /**
     * Cập nhật thông tin giá trị phiên đấu giá trên RAM sau khi Database đã ghi nhận Bid
     */
    public synchronized void updateRamAfterBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidderId);
        }
    }
}