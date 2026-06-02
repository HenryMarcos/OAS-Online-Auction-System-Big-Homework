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
import com.groupproject.shared.network.AuctionEvent.AuctionCancelledEvent;
import com.groupproject.shared.network.events.AuctionEndedEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionFinisedEvent;
import com.groupproject.shared.network.events.AuctionListUpdateEvent;
import com.groupproject.shared.network.events.BalanceUpdateEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionStartedEvent;
import com.groupproject.shared.network.events.NewBidEvent;
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

    private void runHousekeepingTask() {
        ServerLogger.info("--- [Housekeeping] Starting system-wide cleanup scan... ---");
        
        try {
            LocalDateTime now = LocalDateTime.now();

            List<Integer> expiredWaitingIds = AuctionDAO.getExpiredWaitingAuctions(now.minusMinutes(15));
            for (int id : expiredWaitingIds) {
                ServerLogger.info("[Housekeeping] Cancelling expired WAITING auction: " + id);
                forceCancelWaitingAuction(id); 
            }

            List<Integer> missedScheduledIds = AuctionDAO.getMissedScheduledAuctions(now);
            for (int id : missedScheduledIds) {
                ServerLogger.info("[Housekeeping] Auto-starting missed SCHEDULED auction: " + id);
                startAuction(id); 
            }

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
            List<Auction> activeAuctionList = AuctionDAO.getAuctionsByStatus(AuctionStatus.ACTIVED);
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

    private void registerAuctionInternal(Auction auction) {
        activeAuctions.put(auction.getId(), auction);

        if (auction.getStatus() == AuctionStatus.ACTIVED) {
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

    public void activateWaitingAuction(Auction updatedAuction) {
        activeAuctions.put(updatedAuction.getId(), updatedAuction);

        long delayEnd = Duration.between(LocalDateTime.now(), updatedAuction.getEndTime()).toSeconds();
        if (delayEnd < 0) { delayEnd = 0; }
        
        scheduledAuctionIds.add(updatedAuction.getId());
        scheduler.schedule(() -> finishAuction(updatedAuction.getId()), delayEnd, TimeUnit.SECONDS);

        ServerLogger.info("Auction " + updatedAuction.getId() + " manually ACTIVATED. End timer set in " + delayEnd + "s.");
        
        AuctionStartedEvent event = new AuctionStartedEvent(updatedAuction.getId());
        ClientManager.INSTANCE.broadcastSystemEvent(event);

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

    private void startAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        scheduledAuctionIds.remove(auctionId);
        if (auction != null && auction.getStatus() == AuctionStatus.SCHEDULED) {
            LocalDateTime now = LocalDateTime.now();
            boolean isUpdatedInDb = AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.ACTIVED);
            if (isUpdatedInDb) {
                auction.setStatus(AuctionStatus.ACTIVED);
                auction.setStartTime(now); 
                
                long delayEnd = Duration.between(now, auction.getEndTime()).toSeconds();
                if (delayEnd < 0) { delayEnd = 0; }
                
                scheduledAuctionIds.add(auctionId);
                scheduler.schedule(() -> finishAuction(auctionId), delayEnd, TimeUnit.SECONDS);
                
                ServerLogger.info("Auction " + auctionId + " auto-started from SCHEDULED. End timer set in " + delayEnd + "s.");
                
                AuctionStartedEvent event = new AuctionStartedEvent(auctionId);
                ClientManager.INSTANCE.broadcastSystemEvent(event);
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, event);

            } else {
                ServerLogger.error("Failed to auto-start SCHEDULED auction " + auctionId + " in Database.");
            }
        }
    }

    private void finishAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        scheduledAuctionIds.remove(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVED) return;

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

                AuctionEndedEvent endedEvent = new AuctionEndedEvent(auctionId, winnerId, finalPrice);
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, endedEvent);
                ClientManager.INSTANCE.broadcastSystemEvent(endedEvent);

                
                List<Integer> participantIds = BidDAO.getUniqueBidders(auctionId);
                for (Integer userId : participantIds) {
                    String message;
                    if (userId == winnerId) {
                        message = "🏆 You WON the auction for '" + auction.getTitle() + "' with a bid of $" + finalPrice + "!";
                    } else {
                        message = "❌ You lost the auction for '" + auction.getTitle() + "'. It sold for $" + finalPrice + ".";
                    }
                    NotificationDAO.createNotification(userId, message);
                }

                ClientManager.INSTANCE.removeAuctionRoom(auctionId);
            }
        }
    }

    private void cancelFinishedAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null && auction.getStatus() == AuctionStatus.FINISHED) {
            boolean isCancelledInDb = AuctionDAO.updateAuctionStatusOnly(auctionId, AuctionStatus.CANCELLED);
            if (isCancelledInDb) {
                auction.setStatus(AuctionStatus.CANCELLED);
                activeAuctions.remove(auctionId);
                ServerLogger.info("Auction " + auctionId + " fully CANCELLED and cleared from RAM.");

                AuctionCancelledEvent cancelledEvent = new AuctionCancelledEvent(auctionId, "Không có người đặt giá hoặc không thể hoàn tất thanh toán.");
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, cancelledEvent);
                ClientManager.INSTANCE.broadcastSystemEvent(cancelledEvent);

                ClientManager.INSTANCE.removeAuctionRoom(auctionId);
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
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVED) {
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
        boolean dbSuccess = BidDAO.insertBid(auctionId, bidderId, bidAmount, previousBidderId, previousBidAmount);
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
    
    public synchronized void updateRamAfterBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidderId);
        }
    }

    public void broadcastAuctionListUpdate() {
        List<Auction> currentAuctions = getActiveAuctionList();
        AuctionListUpdateEvent updateEvent = new AuctionListUpdateEvent(currentAuctions, LocalDateTime.now());
        ClientManager.INSTANCE.broadcastSystemEvent(updateEvent);
        ServerLogger.info("Dispatched live AuctionListUpdateEvent to all connected users.");
    }
}