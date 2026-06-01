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
import com.groupproject.shared.network.AuctionEvent.AuctionCancelledEvent;
import com.groupproject.shared.network.events.AuctionEndedEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionFinisedEvent;
import com.groupproject.shared.network.events.AuctionListUpdateEvent;
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

            boolean transferOk = BidDAO.executeDirectTransfer(winnerId, sellerId, finalPrice);
            if (transferOk) {
                ServerLogger.info("Transfer completed for auction " + auctionId);
                endAuction(auctionId); 
            } else {
                ServerLogger.error("Critical: Transfer failed for auction " + auctionId + ". Buyer insufficient balance.");
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

                AuctionCancelledEvent cancelledEvent = new AuctionCancelledEvent(auctionId, "Không có người đặt giá hoặc người mua không đủ số dư thanh toán.");
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

        if (bidAmount <= auction.getCurrentBid()) { return false; }

        boolean dbSuccess = BidDAO.insertBid(auctionId, bidderId, bidAmount);
        if (dbSuccess) {
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidderId);
            ServerLogger.info("User " + bidderId + " successfully bid $" + bidAmount + " on Auction " + auctionId);

            NewBidEvent roomUpdate = new NewBidEvent(auctionId, bidAmount, bidderId);
            ClientManager.INSTANCE.broadcastEventToAuction(auctionId, roomUpdate);
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