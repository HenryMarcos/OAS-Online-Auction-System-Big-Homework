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

import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.events.AuctionListUpdateEvent;
import com.groupproject.shared.network.requests.PlaceBidRequest;

public enum AuctionManager {
    INSTANCE;
    
    // Sử dụng ConcurrentSkipListMap để danh sách luôn tự động được sắp xếp theo Auction ID
    private final ConcurrentSkipListMap<Integer, Auction> activeAuctions = new ConcurrentSkipListMap<>();

    // Tracks active auction scheduler tasks to prevent the Duplicate Scheduler Bug
    private final Set<Integer> scheduledAuctionIds = ConcurrentHashMap.newKeySet();

    // Xử lý tất cả phần thời gian đấu giá của các phiên đấu giá
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private AuctionManager() {}

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

        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVATED) {
            ServerLogger.warning("Bid rejected: Auction " + auctionId + " is not active.");
            return false;
        }

        // Kiểm tra bid
        if (bidAmount <= auction.getCurrentBid() || bidAmount < auction.getStartingPrice()) {
            return false;
        }

        // Update bid trong database
        boolean dbSuccess = AuctionDAO.updateBid(auctionId, bidderId, bidAmount);

        if (dbSuccess) {
            // Update bộ nhớ nếu thành công
            auction.setCurrentBid(bidAmount);
            auction.setHighestBidderId(bidderId);
            ServerLogger.info("User " + bidderId + " successfully bid $" + bidAmount + " on Auction " + auctionId);

            // TODO: Broadcast NewBidEvent cho toàn bộ user trong phòng đấu giá
            broadcastAuctionListUpdate();

            return true;
        } else {
            ServerLogger.error("Failed to save bid to DB for Auction " + auctionId);
            return false;
        }
    }

    // Xử lý việc đặt bid(Lấy dữ liệu dưới dạng PlaceBidRequest)
    // ---------------------------------------------------------
    public synchronized boolean placeBid(PlaceBidRequest request) {
        if (request == null) return false;
        return placeBid(request.getAuctionId(), request.getBidderId(), request.getBidAmount());
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
            
            // 📢 Broadcast update so the card vanishes or switches state on client feeds
            broadcastAuctionListUpdate();
        }
    }

    public void broadcastAuctionListUpdate() {
        List<Auction> currentAuctions = getActiveAuctionList();
        AuctionListUpdateEvent updateEvent = new AuctionListUpdateEvent(currentAuctions);
        
        // Dispatches through your thread-safe systemic broadcast mechanism
        ClientManager.INSTANCE.broadcastSystemEvent(updateEvent);
        ServerLogger.info("Dispatched live AuctionListUpdateEvent to all connected users.");
    }

}

