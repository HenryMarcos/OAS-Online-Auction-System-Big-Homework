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
import com.groupproject.server.dao.UserDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.events.AuctionCancelledEvent;
import com.groupproject.shared.network.events.AuctionEndedEvent;
import com.groupproject.shared.network.events.AuctionFinisedEvent;
import com.groupproject.shared.network.events.AuctionFinishedEvent;
import com.groupproject.shared.network.events.AuctionListUpdateEvent;
import com.groupproject.shared.network.events.AuctionStartedEvent;
import com.groupproject.shared.network.events.BalanceUpdateEvent;
import com.groupproject.shared.network.events.NewBidEvent;
import com.groupproject.shared.network.events.SystemNotificationEvent;
import com.groupproject.shared.network.requests.PlaceBidRequest;
import com.groupproject.shared.network.requests.PlaceBidRequest;

public enum AuctionManager {
    INSTANCE;
    
    // Sử dụng ConcurrentSkipListMap để danh sách luôn tự động được sắp xếp theo Auction ID
    private final ConcurrentSkipListMap<Integer, Auction> activeAuctions = new ConcurrentSkipListMap<>();

    // Tracks active auction scheduler tasks to prevent the Duplicate Scheduler Bug
    private final Set<Integer> scheduledAuctionIds = ConcurrentHashMap.newKeySet();

    // Xử lý tất cả phần thời gian đấu giá của các phiên đấu giá
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

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
    
    public void refreshCache() {
        ServerLogger.info("Refreshing AuctionManager Cache from Database...");
        loadActiveAuctionsFromDatabase();
        broadcastAuctionListUpdate();
    }

    // Lấy các phiên đấu giá có đang hoạt động hoặc đang đặt lịch
    // ----------------------------------------------------------
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
        try {
            List<Auction> activeAuctionList = AuctionDAO.getAuctionsByStatus(AuctionStatus.ACTIVATED);
            List<Auction> waitingAuctionList = AuctionDAO.getAuctionsByStatus(AuctionStatus.WAITING);
            List<Auction> scheduledAuctionList = AuctionDAO.getAuctionsByStatus(AuctionStatus.SCHEDULED);
            
            List<Auction> allAuctions = new ArrayList<>();
            if (activeAuctionList != null) allAuctions.addAll(activeAuctionList);
            if (waitingAuctionList != null) allAuctions.addAll(waitingAuctionList);
            if (scheduledAuctionList != null) allAuctions.addAll(scheduledAuctionList);

            List<Integer> fetchedIds = new ArrayList<>();

            for (Auction auction : allAuctions) {
                fetchedIds.add(auction.getId());
                registerAuctionInternal(auction);
            }

            activeAuctions.keySet().removeIf(id -> !fetchedIds.contains(id));
            
        } catch (Exception e) {
            ServerLogger.error("Error loading active auctions from database: " + e.getMessage());
        }
    }

    public synchronized void registerNewAuction(Auction auction) {
        if (auction == null) return;
        ServerLogger.info("Registering newly created auction ID: " + auction.getId());
        registerAuctionInternal(auction);
        broadcastAuctionListUpdate(); 
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
                
                scheduler.schedule(() -> finishAuction(auctionId), delayInSeconds, TimeUnit.SECONDS);
                ServerLogger.info("Auction " + auctionId + " expiration countdown set for " + delayInSeconds + "s.");
            }
        } else if (auction.getStatus() == AuctionStatus.SCHEDULED) {
            if (!scheduledAuctionIds.contains(auction.getId())) {
                long delayStart = Duration.between(LocalDateTime.now(), auction.getStartTime()).toSeconds();
                int auctionId = auction.getId();
                scheduledAuctionIds.add(auctionId);
                scheduler.schedule(() -> startAuction(auctionId), Math.max(0, delayStart), TimeUnit.SECONDS);
                ServerLogger.info("Auction " + auction.getId() + " is SCHEDULED. Start timer set in " + delayStart + "s.");
            }
        }
    }

    public void forceCancelWaitingAuction(int auctionId) {
        Auction auction = activeAuctions.remove(auctionId);
        scheduledAuctionIds.remove(auctionId);
        if (auction != null) {
            // ESCROW: Hoàn tiền nếu đã có người đặt giá (an toàn phòng trường hợp bất thường)
            if (auction.getHighestBidderId() != null && auction.getCurrentBid() > 0) {
                boolean refundOk = UserDAO.addBalance(auction.getHighestBidderId(), auction.getCurrentBid());
                ServerLogger.info("[Escrow] Refunded $" + auction.getCurrentBid()
                        + " to bidder " + auction.getHighestBidderId() + " (force-cancel WAITING)"
                        + (refundOk ? " OK" : " FAILED!"));
                if (refundOk) {
                    double newBal = UserDAO.getBalance(auction.getHighestBidderId());
                    ClientManager.INSTANCE.sendToUser(auction.getHighestBidderId(), new BalanceUpdateEvent(auction.getHighestBidderId(), newBal));
                }
            }

            boolean isUpdated = AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.CANCELLED);
            if (isUpdated) {
                auction.setStatus(AuctionStatus.CANCELLED);
                ServerLogger.info("Auction " + auctionId + " forced to CANCELLED because it expired in WAITING state.");

                AuctionCancelledEvent event = new AuctionCancelledEvent(auctionId, "Phiên đấu giá bị hệ thống hủy do không được bắt đầu đúng hạn.");
                ClientManager.INSTANCE.broadcastSystemEvent(event);
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, event);

                ClientManager.INSTANCE.removeAuctionRoom(auctionId);
            }
        }
    }

    public void cancelAuction(int auctionId) {
        Auction auction = activeAuctions.remove(auctionId);
        scheduledAuctionIds.remove(auctionId);
        if (auction != null) {
            // ESCROW: Hoàn tiền cho người đang giữ giá cao nhất trước khi hủy
            if (auction.getHighestBidderId() != null && auction.getCurrentBid() > 0) {
                boolean refundOk = UserDAO.addBalance(auction.getHighestBidderId(), auction.getCurrentBid());
                ServerLogger.info("[Escrow] Refunded $" + auction.getCurrentBid()
                        + " to bidder " + auction.getHighestBidderId() + " (manual cancel)"
                        + (refundOk ? " OK" : " FAILED!"));
                if (refundOk) {
                    double newBal = UserDAO.getBalance(auction.getHighestBidderId());
                    ClientManager.INSTANCE.sendToUser(auction.getHighestBidderId(), new BalanceUpdateEvent(auction.getHighestBidderId(), newBal));
                }
            }

            boolean isUpdated = AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.CANCELLED);
            if (isUpdated) {
                auction.setStatus(AuctionStatus.CANCELLED);
                ServerLogger.info("Auction " + auctionId + " manually CANCELLED and removed from active RAM memory.");

                AuctionCancelledEvent cancelledEvent = new AuctionCancelledEvent(auctionId, "Phiên đấu giá đã bị hủy bởi người bán hoặc quản trị viên.");
                ClientManager.INSTANCE.broadcastSystemEvent(cancelledEvent);
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, cancelledEvent);

                ClientManager.INSTANCE.removeAuctionRoom(auctionId);
            }
        }
    }

    private void finishAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        scheduledAuctionIds.remove(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVATED) return;

        if (AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.FINISHED)) {
            auction.setStatus(AuctionStatus.FINISHED);
            ServerLogger.info("Auction " + auctionId + " time count end. Status changed to FINISHED.");

            AuctionFinisedEvent finishedEvent = new AuctionFinisedEvent(auctionId, auction.getHighestBidderId() != null ? auction.getHighestBidderId() : 0, auction.getCurrentBid());
            ClientManager.INSTANCE.broadcastEventToAuction(auctionId, finishedEvent);
            ClientManager.INSTANCE.broadcastSystemEvent(finishedEvent);


            double finalPrice = auction.getCurrentBid();
            Integer winnerId = auction.getHighestBidderId();
            int sellerId = auction.getSellerId();

            if (finalPrice <= 0 || winnerId == null) {
                ServerLogger.info("No bids for auction " + auctionId + ". Automatically cancelling...");
                cancelFinishedAuction(auctionId);
                return;
            }

            // ESCROW: Tiền winner đã bị trừ lúc đặt giá, chỉ cần cộng cho seller
            boolean payOk = UserDAO.addBalance(sellerId, finalPrice);
            if (payOk) {
                ServerLogger.info("[Escrow] Paid $" + finalPrice + " to seller " + sellerId + " for auction " + auctionId);
                // Cập nhật số dư realtime cho seller
                double sellerNewBal = UserDAO.getBalance(sellerId);
                ClientManager.INSTANCE.sendToUser(sellerId, new BalanceUpdateEvent(sellerId, sellerNewBal));
                endAuction(auctionId);
            } else {
                // Không thể trả tiền cho seller (lỗi DB nghiêm trọng): hoàn tiền winner và hủy
                ServerLogger.error("Critical: Failed to pay seller for auction " + auctionId + ". Refunding winner " + winnerId);
                boolean refundWinnerOk = UserDAO.addBalance(winnerId, finalPrice);
                if (refundWinnerOk) {
                    double winnerNewBal = UserDAO.getBalance(winnerId);
                    ClientManager.INSTANCE.sendToUser(winnerId, new BalanceUpdateEvent(winnerId, winnerNewBal));
                }
                cancelFinishedAuction(auctionId);
            }
        }
    }



    public List<Auction> getActiveAuctionList() {
        ServerLogger.info("Getting activated auction list");
        if (activeAuctions.isEmpty()) {
            ServerLogger.warning("Found no activated auction");
            return new ArrayList<>(); 
        } 
        List<Auction> activeAuctionList = new ArrayList<>(activeAuctions.values());
        ServerLogger.info("Finish getting activated auction list with " + activeAuctionList.size() + " auctions");
        return activeAuctionList;
    }

    public synchronized boolean placeBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVATED) {
            ServerLogger.warning("Bid rejected: Auction " + auctionId + " is not active.");
            return false;
        }

        double minimalRequired = Math.max(auction.getCurrentBid(), auction.getStartingPrice());
        if (bidAmount <= minimalRequired) {
            ServerLogger.warning("Bid rejected: Amount $" + bidAmount + " is too low.");
            return false;
        }

        // Lưu lại thông tin người đang giữ giá cũ trước khi ghi đè (dùng cho ESCROW refund)
        Integer previousBidderId  = auction.getHighestBidderId();
        double  previousBidAmount = auction.getCurrentBid();

        // insertBid xử lý toàn bộ escrow (double-check, trừ tiền mới, hoàn tiền cũ) trong 1 transaction
        boolean dbSuccess = BidDAO.insertBid2(auctionId, bidderId, bidAmount, previousBidderId, previousBidAmount);
        if (dbSuccess) {
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidderId);
            ServerLogger.info("User " + bidderId + " successfully bid $" + bidAmount + " on Auction " + auctionId);

            // Cập nhật số dư realtime cho người vừa đặt giá (tiền bị trừ)
            double bidderNewBal = UserDAO.getBalance(bidderId);
            ClientManager.INSTANCE.sendToUser(bidderId, new BalanceUpdateEvent(bidderId, bidderNewBal));

            // Auto-subscribe người vừa trở thành highest bidder vào phòng
            ClientManager.INSTANCE.subscribeUserToAuction(auctionId, bidderId);

            // BƯỚC 1: Broadcast NewBidEvent cho toàn phòng TRƯỚC (kể cả người bị outbid vẫn còn trong phòng)
            NewBidEvent roomUpdate = new NewBidEvent(auctionId, bidAmount, bidderId);
            ClientManager.INSTANCE.broadcastEventToAuction(auctionId, roomUpdate);

            // BƯỚC 2: Sau khi người bị outbid đã nhận được thông báo, mới unsubscribe họ ra khỏi phòng
            if (previousBidderId != null && previousBidderId != bidderId) {
                ClientManager.INSTANCE.unsubscribeUserFromAuction(auctionId, previousBidderId);
                // Cập nhật số dư cho người bị outbid (tiền được hoàn)
                double prevBidderNewBal = UserDAO.getBalance(previousBidderId);
                ClientManager.INSTANCE.sendToUser(previousBidderId, new BalanceUpdateEvent(previousBidderId, prevBidderNewBal));
            }

            broadcastAuctionListUpdate();
            return true;
        } else {
            ServerLogger.error("Failed to save bid to DB for Auction " + auctionId);
            return false;
        }
    }


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

        scheduledAuctionIds.add(updatedAuction.getId());
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