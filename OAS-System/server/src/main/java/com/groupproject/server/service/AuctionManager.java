package com.groupproject.server.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.dao.BidDAO;
import com.groupproject.server.dao.NotificationDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.events.AuctionCancelledEvent;
import com.groupproject.shared.network.events.AuctionEndedEvent;
import com.groupproject.shared.network.events.AuctionFinishedEvent;
import com.groupproject.shared.network.events.AuctionListUpdateEvent;
import com.groupproject.shared.network.events.AuctionStartedEvent;
import com.groupproject.shared.network.events.NewBidEvent;
import com.groupproject.shared.network.events.SystemNotificationEvent;
import com.groupproject.shared.network.requests.PlaceBidRequest;

public enum AuctionManager {
    INSTANCE;
    
    // Sử dụng ConcurrentSkipListMap để danh sách luôn tự động được sắp xếp theo Auction ID
    private final ConcurrentSkipListMap<Integer, Auction> activeAuctions = new ConcurrentSkipListMap<>();

    // Tracks active auction scheduler tasks to prevent the Duplicate Scheduler Bug
    private final Set<Integer> scheduledAuctionIds = ConcurrentHashMap.newKeySet();

    // Xử lý tất cả phần thời gian đấu giá của các phiên đấu giá
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    public void refreshCache() {
        ServerLogger.info("Refreshing AuctionManager Cache from Database...");
        loadActiveAuctionsFromDatabase();
        broadcastAuctionListUpdate();
    }

    // Lấy các phiên đấu giá có đang hoạt động hoặc đang đặt lịch
    // ----------------------------------------------------------
    private void loadActiveAuctionsFromDatabase() {
        try {
            List<Auction> activeAuctionList = AuctionDAO.getAuctionsByStatus(AuctionStatus.ACTIVATED);
            List<Auction> waitingAuctionList = AuctionDAO.getAuctionsByStatus(AuctionStatus.WAITING);
            
            List<Auction> allAuctions = new ArrayList<>();
            if (activeAuctionList != null) allAuctions.addAll(activeAuctionList);
            if (waitingAuctionList != null) allAuctions.addAll(waitingAuctionList);

            List<Integer> fetchedIds = new ArrayList<>();

            for (Auction auction : allAuctions) {
                fetchedIds.add(auction.getId());
                if (auction.getStatus() == AuctionStatus.ACTIVATED || auction.getStatus() == AuctionStatus.WAITING) {
                    registerAuctionInternal(auction);
                }
            }

            // Purge items from memory cache if they are no longer marked active in the database
            activeAuctions.keySet().removeIf(id -> !fetchedIds.contains(id));
            
        } catch (Exception e) {
            ServerLogger.error("Error loading active auctions from database: " + e.getMessage());
        }
    }

    // Đăng ký phiên đấu giá mới
    // -------------------------
    public synchronized void registerNewAuction(Auction auction) {
        if (auction == null) return;
        ServerLogger.info("Registering newly created auction ID: " + auction.getId());
        registerAuctionInternal(auction);
        broadcastAuctionListUpdate(); // 📢 Push real-time event
    }

    public List<Auction> getUserAuctions(int userId) {
        return AuctionDAO.getAuctionsBySellerId(userId);
    }

    // Kết nối với hệ thống lập lịch mà không bị trùng lặp task
    // --------------------------------------------------------
    private void registerAuctionInternal(Auction auction) {
        activeAuctions.put(auction.getId(), auction);

        if (auction.getStatus() == AuctionStatus.ACTIVATED) {
            if (!scheduledAuctionIds.contains(auction.getId())) {
                long delayInSeconds = Duration.between(LocalDateTime.now(), auction.getEndTime()).toSeconds();
                if (delayInSeconds < 0) delayInSeconds = 0;

                int auctionId = auction.getId();
                scheduledAuctionIds.add(auctionId);
                
                scheduler.schedule(() -> endAuction(auctionId), delayInSeconds, TimeUnit.SECONDS);
                ServerLogger.info("Auction " + auctionId + " expiration countdown set for " + delayInSeconds + "s.");
            }
        }
    }

    // Hàm lấy các phiên đấu giá dưới dạng List để gửi cho client
    // ----------------------------------------------------------
    public List<Auction> getActiveAuctionList() {
        ServerLogger.info("Getting activated auction list");
        if (activeAuctions.isEmpty()) {
            ServerLogger.warning("Found no activated auction");
            return new ArrayList<>(); // Return empty list instead of null
        } 

        List<Auction> activeAuctionList = new ArrayList<>(activeAuctions.values());
        ServerLogger.info("Finish getting activated auction list with " + activeAuctionList.size() + " auctions");
        return activeAuctionList;
    }

    // Xử lý việc đặt bid
    // ------------------
    public synchronized boolean placeBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);

        // Kiểm tra tính hợp lệ
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVATED) {
            ServerLogger.warning("Bid rejected: Auction " + auctionId + " is not active.");
            return false;
        }

        double minimalRequired = Math.max(auction.getCurrentBid(), auction.getStartingPrice());
        if (bidAmount <= minimalRequired) {
            ServerLogger.warning("Bid rejected: Amount $" + bidAmount + " is too low.");
            return false;
        }

        // Kiểm tra bid
        if (bidAmount <= auction.getCurrentBid()) {
            return false;
        }

        // Update bid trong database
        boolean dbSuccess = BidDAO.insertBid(auctionId, bidderId, bidAmount);

        if (dbSuccess) {
            // Update bộ nhớ nếu thành công
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidderId);
            ServerLogger.info("User " + bidderId + " successfully bid $" + bidAmount + " on Auction " + auctionId);

            // 4. BROADCAST 1: Fast update to ONLY people inside the auction room
            NewBidEvent roomUpdate = new NewBidEvent(auctionId, bidAmount, bidderId);
            ClientManager.INSTANCE.broadcastEventToAuction(auctionId, roomUpdate);

            // 5. BROADCAST 2: General update to everyone else for Home Screen cards
            broadcastAuctionListUpdate();

            return true;
        } else {
            ServerLogger.error("Failed to save bid to DB for Auction " + auctionId);
            return false;
        }
    }

    // Xử lý việc đặt bid(Lấy dữ liệu dưới dạng PlaceBidRequest)
    // ---------------------------------------------------------
    public synchronized boolean placeBid(PlaceBidRequest request, ClientHandler clientContext) {
        if (request == null || clientContext.getAuthenticatedUserId() == null) return false;
        return placeBid(request.getAuctionId(), clientContext.getAuthenticatedUserId(), request.getBidAmount());
    }
    
    public Auction getAuction(int auctionId) {
        return activeAuctions.get(auctionId);
    }

    private void endAuction(int auctionId) {
        Auction auction = activeAuctions.remove(auctionId);
        scheduledAuctionIds.remove(auctionId);
        
        if (auction != null) {
            ServerLogger.info("Auction " + auctionId + " reached deadline. Concluding automatically.");
            auction.setStatus(AuctionStatus.ENDED);
            AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.ENDED);
            
            // 1. DETERMINE THE WINNER
            Integer winnerId = auction.getHighestBidderId();
            double winningBid = auction.getCurrentBid();

            // 2. BROADCAST TO THE ROOM SO THEIR UI LOCKS UP INSTANTLY
            AuctionEndedEvent endedEvent = new AuctionEndedEvent(auctionId, winnerId, winningBid);
            ClientManager.INSTANCE.broadcastEventToAuction(auctionId, endedEvent);

            // 🌟 2. PERSISTENT INBOX: Save a notification for everyone who participated
            List<Integer> participantIds = BidDAO.getUniqueBidders(auctionId);
            for (Integer userId : participantIds) {
                String message;
                if (userId == auction.getHighestBidderId()) {
                    message = "🏆 You WON the auction for '" + auction.getTitle() + "' with a bid of $" + auction.getCurrentBid() + "!";
                } else {
                    message = "❌ You lost the auction for '" + auction.getTitle() + "'. It sold for $" + auction.getCurrentBid() + ".";
                }
                // Save to database permanently!
                NotificationDAO.createNotification(userId, message);
            }

            // LUỒNG 3: Ghi nhận Log hoàn tất giao dịch cho Admin
            SystemNotificationEvent adminLog = new SystemNotificationEvent(
                "Phiên ID " + auctionId + " thành công mỹ mãn. Winner ID: " + winnerId + " chốt giá " + winningBid, "Hệ Thống"
            );
            ClientManager.INSTANCE.broadcastToAdmins(adminLog);

            // Giải tán phòng đấu giá hoàn toàn
            ClientManager.INSTANCE.removeAuctionRoom(auctionId);
            // =====================================================================

            // 3. Broadcast global update so the card vanishes on the Home Screen
            broadcastAuctionListUpdate();
        }
    }


    public void broadcastAuctionListUpdate() {
        List<Auction> currentAuctions = getActiveAuctionList();
        AuctionListUpdateEvent updateEvent = new AuctionListUpdateEvent(currentAuctions, LocalDateTime.now());
        
        // Dispatches through your thread-safe systemic broadcast mechanism
        ClientManager.INSTANCE.broadcastSystemEvent(updateEvent);
        ServerLogger.info("Dispatched live AuctionListUpdateEvent to all connected users.");
    }

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


    // Thêm một phiên đấu giá mới vào hệ thống (dùng khi tạo mới hoặc load từ DB)
    public void registerAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);

        if (auction.getStatus() == AuctionStatus.ACTIVATED) {
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
        ClientManager.INSTANCE.broadcastSystemEvent(event);

        // LUỒNG 3: Báo cho Admin lưu log hiển thị hệ thống
        SystemNotificationEvent adminLog = new SystemNotificationEvent(
            "Người bán đã kích hoạt thủ công phiên đấu giá ID: " + updatedAuction.getId(), "Hệ Thống"
        );
        ClientManager.INSTANCE.broadcastToAdmins(adminLog);
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
                ClientManager.INSTANCE.broadcastSystemEvent(event);

                // LUỒNG 2: Phát thông báo cho những người đang ở trong phòng (nếu có)
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, event);
                
                // LUỒNG 3: Ghi log cho Admin
                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Hệ thống ép hủy phiên WAITING quá hạn (ID: " + auctionId + ")", "Hệ Thống"
                );
                ClientManager.INSTANCE.broadcastToAdmins(adminLog);

                // 4. Giải phóng phòng đấu giá
                ClientManager.INSTANCE.removeAuctionRoom(auctionId);
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
                ClientManager.INSTANCE.broadcastSystemEvent(cancelledEvent);

                // LUỒNG 2: Phát thông báo trực tiếp cho những người đang trong phòng
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, cancelledEvent);
                
                // LUỒNG 3: Gửi Log hệ thống báo cho Admin
                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Phiên đấu giá ID " + auctionId + " bị hủy thủ công bởi người bán hoặc Admin.", "Hệ Thống"
                );
                ClientManager.INSTANCE.broadcastToAdmins(adminLog);

                // Giải phóng phòng đấu giá khỏi RAM
                ClientManager.INSTANCE.removeAuctionRoom(auctionId);
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
            
            boolean isUpdatedInDb = AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.ACTIVATED, now);
            
            if (isUpdatedInDb) {
                auction.setStatus(AuctionStatus.ACTIVATED);
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
                ClientManager.INSTANCE.broadcastSystemEvent(event);

                // LUỒNG 2: Báo cho những người đã trực trong phòng từ trước
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, event);

                // LUỒNG 3: Báo cho Admin lưu log sự kiện
                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Hệ thống tự động kích hoạt phiên đấu giá lên lịch thành công (ID: " + auctionId + ")", "Hệ Thống"
                );
                ClientManager.INSTANCE.broadcastToAdmins(adminLog);
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
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVATED) return;

        if (AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.FINISHED)) {
            auction.setStatus(AuctionStatus.FINISHED);

            ServerLogger.info("Auction " + auctionId + " time count end. Status changed to FINISHED.");

            // =====================================================================
            // [RÁP EVENT] PHIÊN HẾT GIỜ (CHỜ XỬ LÝ THANH TOÁN)
            // =====================================================================
            AuctionFinishedEvent finishedEvent = new AuctionFinishedEvent(auctionId);

            // LUỒNG 2: Báo khẩn cấp trong phòng để Client đóng băng nút Bid và hiển thị "Đang xử lý giao dịch..."
            ClientManager.INSTANCE.broadcastEventToAuction(auctionId, finishedEvent);

            // LUỒNG 1: Báo toàn Server để dẹp thẻ này khỏi Main UI (Lobby) người dùng thường
            ClientManager.INSTANCE.broadcastSystemEvent(finishedEvent);

            // LUỒNG 3: Báo Admin ghi nhận log hệ thống chuẩn bị đối soát
            SystemNotificationEvent adminLog = new SystemNotificationEvent(
                "Phiên ID " + auctionId + " đã hết giờ. Bắt đầu đối soát giao dịch ví...", "Hệ Thống"
            );
            ClientManager.INSTANCE.broadcastToAdmins(adminLog);
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
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, cancelledEvent);

                // LUỒNG 1: Báo toàn hệ thống gỡ hoàn toàn thẻ đấu giá này
                ClientManager.INSTANCE.broadcastSystemEvent(cancelledEvent);

                // LUỒNG 3: Báo cho Admin log hệ thống
                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Phiên ID " + auctionId + " bị hệ thống hủy. Lý do: Không ai Bid hoặc ví của người thắng không đủ tiền.", "Hệ Thống"
                );
                ClientManager.INSTANCE.broadcastToAdmins(adminLog);

                // Giải tán phòng đấu giá
                ClientManager.INSTANCE.removeAuctionRoom(auctionId);
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