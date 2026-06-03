package com.groupproject.server.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.groupproject.shared.model.transaction.AuctionDetail;
import com.groupproject.shared.network.events.AuctionCancelledEvent;
import com.groupproject.shared.network.events.AuctionEndedEvent;
import com.groupproject.shared.network.events.AuctionFinisedEvent;
import com.groupproject.shared.network.events.AuctionListUpdateEvent;
import com.groupproject.shared.network.events.AuctionStartedEvent;
import com.groupproject.shared.network.events.BalanceUpdateEvent;
import com.groupproject.shared.network.events.NewBidEvent;
import com.groupproject.shared.network.events.SystemNotificationEvent;
import com.groupproject.shared.network.requests.CreateAuctionRequest;
import com.groupproject.shared.network.requests.PlaceBidRequest;

public enum AuctionManager {
    INSTANCE;
    
    private final ConcurrentSkipListMap<Integer, Auction> activeAuctions = new ConcurrentSkipListMap<>();
    private final Set<Integer> scheduledAuctionIds = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    // A simple cache tracking the last created auction per seller to prevent rapid duplicate inserts
    private final Map<Integer, Long> lastSubmissionTimePerSeller = new ConcurrentHashMap<>();
    private final Map<Integer, String> lastSubmissionTitlePerSeller = new ConcurrentHashMap<>();

    private AuctionManager() {
        loadActiveAuctionsFromDatabase();
        
        scheduler.scheduleAtFixedRate(
            this::runHousekeepingTask, 
            1, 5, TimeUnit.MINUTES 
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
            for (int id : expiredWaitingIds) forceCancelWaitingAuction(id);

            List<Integer> missedScheduledIds = AuctionDAO.getMissedScheduledAuctions(now);
            for (int id : missedScheduledIds) startAuction(id); 

            List<Integer> missedActiveIds = AuctionDAO.getExpiredActiveAuctions(now);
            for (int id : missedActiveIds) finishAuction(id); 

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

    // 🌟 FIX 1: Safe Registration Pipeline
    public synchronized void registerNewAuction(Auction auction) {
        if (auction == null) return;
        ServerLogger.info("Registering newly created auction ID: " + auction.getId());
        
        // Push it into the RAM tracker first so it is available to the system
        registerAuctionInternal(auction);
        
        // Tell everyone a new auction exists
        broadcastAuctionListUpdate(); 
    }

    public List<Auction> getUserAuctions(int userId) {
        return AuctionDAO.getAuctionsBySellerId(userId, null); // Using the proper method name from your DAO
    }

    public AuctionDetail getAuctionDetail(int auctionId) {
        try {
            Auction baseAuction = activeAuctions.get(auctionId);

            if (baseAuction == null) {
                ServerLogger.warning("Auction " + auctionId + " not found in live RAM cache.");
                return null; 
            }

            String description = AuctionDAO.getAuctionDescription(auctionId);
            java.util.Map<Integer, java.util.Map<String, String>> specs = AuctionDAO.getSpecificationsForAuction(auctionId);
            java.util.List<com.groupproject.shared.model.transaction.BidDTO> pastBids = BidDAO.getBidsForAuction(auctionId);

            java.util.List<String> subImagePaths = AuctionDAO.getSubImagePaths(auctionId);
            java.util.List<byte[]> subImageBytes = new java.util.ArrayList<>();
            for (String path : subImagePaths) {
                byte[] img = com.groupproject.server.utils.ImageStorageManager.loadImage(path);
                if (img != null) subImageBytes.add(img);
            }

            return new AuctionDetail(baseAuction, description, subImageBytes, pastBids, specs);

        } catch (Exception e) {
            ServerLogger.error("Failed to assemble AuctionDetail: " + e.getMessage());
            return null;
        }
    }

    public Auction createAuction(CreateAuctionRequest req, int sellerId) {
        long currentTime = System.currentTimeMillis();

        // Check if this seller just pushed an auction with the exact same title in the last 3 seconds
        if (lastSubmissionTitlePerSeller.containsKey(sellerId) && 
            lastSubmissionTitlePerSeller.get(sellerId).equals(req.getTitle())) {
            
            long timeDelta = currentTime - lastSubmissionTimePerSeller.getOrDefault(sellerId, 0L);
            if (timeDelta < 3000) { // 3-second block window
                ServerLogger.warning("Duplicate auction request rejected for Seller ID: " + sellerId);
                return null; // Reject handling duplicate request
            }
        }

        // Record this submission
        lastSubmissionTimePerSeller.put(sellerId, currentTime);
        lastSubmissionTitlePerSeller.put(sellerId, req.getTitle());
        try {
            String mainImagePath = null;
            if (req.getMainImageBytes() != null && req.getMainImageBytes().length > 0) {
                mainImagePath = com.groupproject.server.utils.ImageStorageManager.saveImage(
                    req.getMainImageBytes(), "main_" + sellerId + "_" + System.currentTimeMillis() + ".jpg"
                );
            }

            List<String> subImagePaths = new ArrayList<>();
            if (req.getSubImagesBytes() != null) {
                for (int i = 0; i < req.getSubImagesBytes().size(); i++) {
                    byte[] subBytes = req.getSubImagesBytes().get(i);
                    if (subBytes != null && subBytes.length > 0) {
                        subImagePaths.add(com.groupproject.server.utils.ImageStorageManager.saveImage(
                            subBytes, "sub_" + sellerId + "_" + i + "_" + System.currentTimeMillis() + ".jpg"
                        ));
                    }
                }
            }

            // 🌟 FIX 2: Ensure the returned auction has a proper ID and Time parameters
            Auction newAuction = AuctionDAO.createAuction(
                sellerId, req.getTitle(), mainImagePath, subImagePaths, 
                req.getDescription(), req.getCategory(), req.getCategoryGroupedSpecs(),
                req.getStartingPrice(), req.getDuration(), req.getStartTime(), req.getEndTime(), req.getStatus()
            );

            if (newAuction != null) {
                // Pre-load the bytes so clients online don't have to wait for a DB refresh
                newAuction.setMainImageBytes(req.getMainImageBytes());
                
                // If it's starting immediately, activate its timer and broadcast it
                if (newAuction.getStatus() == AuctionStatus.ACTIVATED || newAuction.getStatus() == AuctionStatus.SCHEDULED) {
                     registerNewAuction(newAuction);
                } else {
                     // Even if it's WAITING, we should still track it in memory so the creator can see it in their list
                     activeAuctions.put(newAuction.getId(), newAuction);
                     broadcastAuctionListUpdate();
                }
                return newAuction;
            }
        } catch (Exception e) {
            ServerLogger.error("Failed to process auction creation: " + e.getMessage());
        }
        return null;
    }

    // 🌟 FIX 3: Robust Null Checking on Dates inside Internal Registration
    private void registerAuctionInternal(Auction auction) {
        ServerLogger.info("Registering auction internal: ID " + auction.getId());
        
        // Safely push to RAM
        activeAuctions.put(auction.getId(), auction);

        if (auction.getStatus() == AuctionStatus.ACTIVATED) {
            if (!scheduledAuctionIds.contains(auction.getId())) {
                if (auction.getEndTime() == null) {
                    ServerLogger.error("CRITICAL: ACTIVATED Auction " + auction.getId() + " has no EndTime! Cannot schedule.");
                    return;
                }
                long delayInSeconds = Duration.between(LocalDateTime.now(), auction.getEndTime()).toSeconds();
                if (delayInSeconds < 0) delayInSeconds = 0;

                int auctionId = auction.getId();
                scheduledAuctionIds.add(auctionId);
                scheduler.schedule(() -> finishAuction(auctionId), delayInSeconds, TimeUnit.SECONDS);
                ServerLogger.info("Auction " + auctionId + " expiration countdown set for " + delayInSeconds + "s.");
            }
        } else if (auction.getStatus() == AuctionStatus.SCHEDULED) {
            if (!scheduledAuctionIds.contains(auction.getId())) {
                if (auction.getStartTime() == null) {
                    ServerLogger.error("CRITICAL: SCHEDULED Auction " + auction.getId() + " has no StartTime! Cannot schedule.");
                    return;
                }
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
            if (auction.getHighestBidderId() != null && auction.getCurrentBid() > 0) {
                boolean refundOk = UserDAO.addBalance(auction.getHighestBidderId(), auction.getCurrentBid());
                if (refundOk) {
                    double newBal = UserDAO.getBalance(auction.getHighestBidderId());
                    ClientManager.INSTANCE.sendToUser(auction.getHighestBidderId(), new BalanceUpdateEvent(auction.getHighestBidderId(), newBal));
                }
            }
            boolean isUpdated = AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.CANCELLED);
            if (isUpdated) {
                auction.setStatus(AuctionStatus.CANCELLED);
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
            if (auction.getHighestBidderId() != null && auction.getCurrentBid() > 0) {
                boolean refundOk = UserDAO.addBalance(auction.getHighestBidderId(), auction.getCurrentBid());
                if (refundOk) {
                    double newBal = UserDAO.getBalance(auction.getHighestBidderId());
                    ClientManager.INSTANCE.sendToUser(auction.getHighestBidderId(), new BalanceUpdateEvent(auction.getHighestBidderId(), newBal));
                }
            }

            boolean isUpdated = AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.CANCELLED);
            if (isUpdated) {
                auction.setStatus(AuctionStatus.CANCELLED);
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

        if (AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.FINISHED)) {
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

            boolean payOk = UserDAO.addBalance(sellerId, finalPrice);
            if (payOk) {
                double sellerNewBal = UserDAO.getBalance(sellerId);
                ClientManager.INSTANCE.sendToUser(sellerId, new BalanceUpdateEvent(sellerId, sellerNewBal));
                endAuction(auctionId);
            } else {
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
        if (activeAuctions.isEmpty()) return new ArrayList<>(); 
        return new ArrayList<>(activeAuctions.values());
    }

    public boolean placeBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVATED) return false;

        double minimalRequired = Math.max(auction.getCurrentBid(), auction.getStartingPrice());
        if (bidAmount <= minimalRequired) return false;

        Integer previousBidderId = auction.getHighestBidderId();

        synchronized (auction) { 
            if (bidAmount <= auction.getCurrentBid()) return false; 

            boolean dbSuccess = BidDAO.insertBid(auctionId, bidderId, bidAmount);

            if (dbSuccess) {
                auction.setCurrentBid(bidAmount);
                auction.setHighestBidderId(bidderId);

                double bidderNewBal = UserDAO.getBalance(bidderId);
                ClientManager.INSTANCE.sendToUser(bidderId, new BalanceUpdateEvent(bidderId, bidderNewBal));

                ClientManager.INSTANCE.subscribeUserToAuction(auctionId, bidderId);

                NewBidEvent roomUpdate = new NewBidEvent(auctionId, bidAmount, bidderId);
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, roomUpdate);

                if (previousBidderId != null && previousBidderId != bidderId) {
                    ClientManager.INSTANCE.unsubscribeUserFromAuction(auctionId, previousBidderId);
                    double prevBidderNewBal = UserDAO.getBalance(previousBidderId);
                    ClientManager.INSTANCE.sendToUser(previousBidderId, new BalanceUpdateEvent(previousBidderId, prevBidderNewBal));
                }

                broadcastAuctionListUpdate();
                return true;
            }
        }
        return false; 
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
            auction.setStatus(AuctionStatus.ENDED);
            AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.ENDED);
            
            Integer winnerId = auction.getHighestBidderId();
            double winningBid = auction.getCurrentBid();

            AuctionEndedEvent endedEvent = new AuctionEndedEvent(auctionId, winnerId, winningBid);
            ClientManager.INSTANCE.broadcastEventToAuction(auctionId, endedEvent);

            List<Integer> participantIds = BidDAO.getUniqueBidders(auctionId);
            for (Integer userId : participantIds) {
                String message = (userId.equals(winnerId)) ? 
                    "🏆 You WON the auction for '" + auction.getTitle() + "' with a bid of $" + winningBid + "!" :
                    "❌ You lost the auction for '" + auction.getTitle() + "'. It sold for $" + winningBid + ".";
                NotificationDAO.createNotification(userId, message);
            }

            SystemNotificationEvent adminLog = new SystemNotificationEvent(
                "Phiên ID " + auctionId + " thành công mỹ mãn. Winner ID: " + winnerId + " chốt giá " + winningBid, "Hệ Thống"
            );
            ClientManager.INSTANCE.broadcastToAdmins(adminLog);

            ClientManager.INSTANCE.removeAuctionRoom(auctionId);
            broadcastAuctionListUpdate();
        }
    }

    public void broadcastAuctionListUpdate() {
        List<Auction> currentAuctions = getActiveAuctionList();
        AuctionListUpdateEvent updateEvent = new AuctionListUpdateEvent(currentAuctions, LocalDateTime.now());
        ClientManager.INSTANCE.broadcastSystemEvent(updateEvent);
    }

    public void activateWaitingAuction(Auction updatedAuction) {
        activeAuctions.put(updatedAuction.getId(), updatedAuction);

        long delayEnd = Duration.between(LocalDateTime.now(), updatedAuction.getEndTime()).toSeconds();
        if (delayEnd < 0) delayEnd = 0;

        scheduledAuctionIds.add(updatedAuction.getId());
        scheduler.schedule(() -> finishAuction(updatedAuction.getId()), delayEnd, TimeUnit.SECONDS);

        AuctionStartedEvent event = new AuctionStartedEvent(updatedAuction.getId());
        ClientManager.INSTANCE.broadcastSystemEvent(event);

        SystemNotificationEvent adminLog = new SystemNotificationEvent(
            "Người bán đã kích hoạt thủ công phiên đấu giá ID: " + updatedAuction.getId(), "Hệ Thống"
        );
        ClientManager.INSTANCE.broadcastToAdmins(adminLog);
    }

    private void startAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        
        if (auction != null && auction.getStatus() == AuctionStatus.SCHEDULED) {
            LocalDateTime now = LocalDateTime.now();
            boolean isUpdatedInDb = AuctionDAO.updateAuctionStatusWithTime(auctionId, AuctionStatus.ACTIVATED, now, null);
            
            if (isUpdatedInDb) {
                auction.setStatus(AuctionStatus.ACTIVATED);
                auction.setStartTime(now); 
                
                long delayEnd = Duration.between(now, auction.getEndTime()).toSeconds();
                if (delayEnd < 0) delayEnd = 0;
                
                scheduler.schedule(() -> finishAuction(auctionId), delayEnd, TimeUnit.SECONDS);
                
                AuctionStartedEvent event = new AuctionStartedEvent(auctionId);
                ClientManager.INSTANCE.broadcastSystemEvent(event);
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, event);

                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Hệ thống tự động kích hoạt phiên đấu giá (ID: " + auctionId + ")", "Hệ Thống"
                );
                ClientManager.INSTANCE.broadcastToAdmins(adminLog);
            }
        }
    }

    private void cancelFinishedAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        
        if (auction != null && auction.getStatus() == AuctionStatus.FINISHED) {
            boolean isCancelledInDb = AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.CANCELLED);
            
            if (isCancelledInDb) {
                auction.setStatus(AuctionStatus.CANCELLED);
                activeAuctions.remove(auctionId); 

                AuctionCancelledEvent cancelledEvent = new AuctionCancelledEvent(auctionId, "Không có người đặt giá hoặc giao dịch lỗi.");
                ClientManager.INSTANCE.broadcastEventToAuction(auctionId, cancelledEvent);
                ClientManager.INSTANCE.broadcastSystemEvent(cancelledEvent);

                SystemNotificationEvent adminLog = new SystemNotificationEvent(
                    "Phiên ID " + auctionId + " bị hủy. Không ai Bid hoặc ví không đủ.", "Hệ Thống"
                );
                ClientManager.INSTANCE.broadcastToAdmins(adminLog);
                ClientManager.INSTANCE.removeAuctionRoom(auctionId);
            }
        }
    }

    public synchronized void updateRamAfterBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidderId);
        }
    }
}