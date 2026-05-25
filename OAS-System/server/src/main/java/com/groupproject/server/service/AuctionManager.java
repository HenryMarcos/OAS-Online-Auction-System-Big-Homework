package com.groupproject.server.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.PlaceBidRequest;

public enum AuctionManager {
    INSTANCE;
    
    // Sử dụng ConcurrentSkipListMap để danh sách luôn tự động được sắp xếp theo Auction ID
    private final ConcurrentSkipListMap<Integer, Auction> activeAuctions = new ConcurrentSkipListMap<>();
    // Xử lý tất cả phần thời gian đấu giá của các phiên đấu giá
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private AuctionManager() {}

    public void refreshCache() {
        loadActiveAuctionsFromDatabase();
    }

    // Lấy các phiên đấu giá đang hoạt động và đăng ký chúng
    // -----------------------------------------------------
    private void loadActiveAuctionsFromDatabase() {
        activeAuctions.clear();
        List<Auction> activeAuctionList = AuctionDAO.getAuctionsByStatus(AuctionStatus.ACTIVATED);

        for (Auction auction : activeAuctionList) {
            registerAuction(auction);
        }
    }

    // Đăng ký phiên đấu giá
    // ---------------------
    public void registerAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);

        // Kiểm tra xem phiên đấu giá có ngay lập tức bắt đầu đếm xuống luôn không
        if (auction.getStatus() == AuctionStatus.ACTIVATED) {
            // Tính khoảng delay trước khi phiên đấu giá kết thúc
            long delayInSeconds = Duration.between(LocalDateTime.now(), auction.getEndTime()).toSeconds();
            if (delayInSeconds < 0) { delayInSeconds = 0; }

            // Lên lịch cho nhiệm vụ đóng phiên đấu giá
            scheduler.schedule(() -> { endAuction(auction.getId()); }, delayInSeconds, TimeUnit.SECONDS);

            ServerLogger.info("Auction " + auction.getId() + " is ACTIVATED. Countdown timer started (" + delayInSeconds + "s).");
        } else {
            ServerLogger.info("Auction " + auction.getId() + " is WAITING. Timer will start once the seller activates it manually.");
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
            return true;
        } else {
            ServerLogger.error("Failed to save bid to DB for Auction " + auctionId);
            return false;
        }
    }

    // Xử lý việc đặt bid(Lấy dữ liệu dưới dạng PlaceBidRequest)
    // ---------------------------------------------------------
    public synchronized boolean placeBid(PlaceBidRequest request) {
        return placeBid(request.getAuctionId(), request.getBidderId(), request.getBidAmount());
    }
    
    public Auction getAuction(int auctionId) {
        return activeAuctions.get(auctionId);
    }

    private void endAuction(int auctionId) {
        Auction auction = activeAuctions.remove(auctionId);
        if (auction != null) {
            ServerLogger.info("Auction " + auctionId + " has officially ended!");

            // Cập nhật memory
            auction.setStatus(AuctionStatus.ENDED);
            
            // Gọi DAO để update database
            AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.ENDED);
            
            // TODO: Broadcast "AUCTION_ENDED" notification to connected clients
        }
    }

}

