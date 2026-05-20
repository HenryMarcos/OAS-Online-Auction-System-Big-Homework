package com.groupproject.server.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.groupproject.server.core.ServerApp;
import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.dao.DatabaseManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.AuctionItem;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.BidRequest;

public class AuctionManager {
    private static AuctionManager instance;

    // Tìm nhanh các phiên đấu giá còn đang hoạt động
    private final ConcurrentHashMap<Integer, Auction> activeAuctions = new ConcurrentHashMap<>();

    // Xử lý tất cả phần thời gian đấu giá của các phiên đấu giá
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private AuctionManager() {

    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) { instance = new AuctionManager(); }
        return instance;
    }

    private void loadActiveAuctionsFromDatabase() {
        // Sử dụng DAO để lấy thông tin các phiên đấu giá và đăng ký những phiên chưa kết thúc.
        for (Auction auction : AuctionDAO.getAuctions()) {
            if (auction.getEndTime().isAfter(LocalDateTime.now())) {
                registerAuction(auction);
            }
        }
    }

    public void registerAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);

        // Tính khoảng delay trước khi phiên đấu giá kết thúc
        long delayInSeconds = Duration.between(LocalDateTime.now(), auction.getEndTime()).toSeconds();
        if (delayInSeconds < 0) { delayInSeconds = 0; }

        // Lên lịch cho nhiệm vụ đóng phiên đấu giá
        scheduler.schedule(() -> {
            endAuction(auction.getId());
        }, delayInSeconds, TimeUnit.SECONDS);

    }

    public synchronized boolean placeBid(int auctionId, int bidderId, double bidAmount) {
        Auction auction = activeAuctions.get(auctionId);

        if (auction == null) {
            ServerLogger.warning("Bid rejected: Auction " + auctionId + " is not active or already closed.");
            return false;
        }

        // Kiểm tra bid
        if (bidAmount <= auction.getCurrentBid() || bidAmount < auction.getStartingPrice()) {
            return false;
        }

        // Update trạng thái trong bộ nhớ

        // TODO: Thông báo cho người dùng có trạng thái cao nhất trước

        auction.setCurrentBid(bidAmount);
        auction.setHighestBidderId(bidderId);

        // TODO: Update trạng thái trong database

        return true;
    }

    public synchronized boolean placeBid(BidRequest request) {
        return placeBid(request.getAuctionId(), )
    }

    public static synchronized boolean proccessBid(int auctionId, String bidderUsername, double bidAmount) {
        String checkSql = "SELECT current_bid, is_active FROM auctions WHERE id = ?";
        String updateSql = "UPDATE auctions SET current_bid = ?, highest_bidder = ? WHERE id = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();

        try {

            // Kiểm tra xem bid hợp lý chưa
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, auctionId);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    double currentBid = rs.getDouble("current_bid");
                    boolean isActive = rs.getBoolean("is_active");

                    // Nếu auction đã kết thúc hoặc bid quá thấp thì từ chối
                    if (!isActive || bidAmount <= currentBid) {
                        ServerApp.log("USER " + bidderUsername + ": auction is not active or bid is too low");
                        return false;
                    }
                } else {
                    ServerApp.log("USER " + bidderUsername + ": auctionId does not exist");
                    return false;
                }
            }

            // Update bid cho các user
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, bidAmount);
                updateStmt.setString(2, bidderUsername);
                updateStmt.setInt(3, auctionId);
                updateStmt.executeQuery();
                return true;
            }

        } catch (Exception e) {
            ServerApp.log("Database error processing bid: " + e.getMessage());
            return false;
        }
    }

    public static synchronized boolean proccessBid(BidRequest bidRequest) {
        return proccessBid(bidRequest.getAuctionId(), bidRequest.getBidderUsername(), bidRequest.getBidAmount());
    }

    public static List<AuctionItem> getActiveAuctions() {
        List<AuctionItem> activeAuctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE is_active = 1";
        Connection conn = DatabaseManager.getInstance().getConnection();

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                AuctionItem item = new AuctionItem(
                    rs.getInt("id"),
                    rs.getString("item_name"), 
                    rs.getDouble("current_bid"), 
                    rs.getString("highest_bidd")
                );

                activeAuctions.add(item);
            }
        } catch (Exception e) {
            ServerApp.log("Error fetching auctions: " + e.getMessage());
        }
        return activeAuctions;
    }
}
