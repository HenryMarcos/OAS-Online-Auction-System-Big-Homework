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

import com.groupproject.server.dao.AuctionDAO;
import com.groupproject.server.dao.DatabaseManager;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.AuctionItem;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.BidRequest;

public class AuctionManager {
    private static AuctionManager instance;

    // Tìm nhanh các phiên đấu giá còn đang hoạt động
    private final ConcurrentHashMap<Integer, Auction> activeAuctions = new ConcurrentHashMap<>();

    // Xử lý tất cả phần thời gian đấu giá của các phiên đấu giá
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // 1. Constructor private để ngăn chặn việc khởi tạo từ bên ngoài
    private AuctionManager() {
        // Khi khởi tạo, load tất cả các phiên đấu giá đang hoạt động từ database vào bộ nhớ
        loadActiveAuctionsFromDatabase();
    }

    // 2. Static inner class chứa instance duy nhất (The Bill Pugh concept)
    private static class AuctionManagerHelper {
        // Biến INSTANCE được khởi tạo và gán là final
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    // 3. Phương thức lấy instance hoàn toàn không cần 'synchronized'
    public static AuctionManager getInstance() {
        return AuctionManagerHelper.INSTANCE;
    }

    private void loadActiveAuctionsFromDatabase() {
        // Chỉ lấy những cái thực sự cần thiết từ Database
        for (Auction auction : AuctionDAO.getActiveAuctions()) {
            registerAuction(auction);
        }
    }

    public void registerAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);

        // Kiểm tra xem phiên đấu giá có ngay lập tức bắt đầu đếm xuống luôn không
        if (auction.getStatus() == AuctionStatus.ACTIVATED) {
            // Tính khoảng delay trước khi phiên đấu giá kết thúc
            long delayInSeconds = Duration.between(LocalDateTime.now(), auction.getEndTime()).toSeconds();
            if (delayInSeconds < 0) { delayInSeconds = 0; }

            // Lên lịch cho nhiệm vụ đóng phiên đấu giá
            scheduler.schedule(() -> {
                endAuction(auction.getId());
            }, delayInSeconds, TimeUnit.SECONDS);

            ServerLogger.info("Auction " + auction.getId() + " is ACTIVATED. Countdown timer started (" + delayInSeconds + "s).");
        } else {
            ServerLogger.info("Auction " + auction.getId() + " is WAITING. Timer will start once the seller activates it manually.");
        }

        

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
        return placeBid(request.getAuctionId(), request.getBidderId(), request.getBidAmount());
    }

    public static synchronized boolean proccessBid(int auctionId, int bidderId, double bidAmount) {
        String checkSql = "SELECT current_bid, is_active FROM auctions WHERE id = ?";
        String updateSql = "UPDATE auctions SET current_bid = ?, highest_bidder = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();) {

            // Kiểm tra xem bid hợp lý chưa
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, auctionId);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    double currentBid = rs.getDouble("current_bid");
                    boolean isActive = rs.getBoolean("is_active");

                    // Nếu auction đã kết thúc hoặc bid quá thấp thì từ chối
                    if (!isActive || bidAmount <= currentBid) {
                        ServerLogger.info("USER " + bidderId + ": auction is not active or bid is too low");
                        return false;
                    }
                } else {
                    ServerLogger.info("USER " + bidderId + ": auctionId does not exist");
                    return false;
                }
            }

            // Update bid cho các user
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, bidAmount);
                updateStmt.setInt(2, bidderId);
                updateStmt.setInt(3, auctionId);
                updateStmt.executeQuery();
                return true;
            }

        } catch (Exception e) {
            ServerLogger.error("Database error processing bid: " + e.getMessage());
            return false;
        }
    }

    public static synchronized boolean proccessBid(BidRequest bidRequest) {
        return proccessBid(bidRequest.getAuctionId(), bidRequest.getBidderId(), bidRequest.getBidAmount());
    }

    public static List<AuctionItem> getActiveAuctions() {
        List<AuctionItem> activeAuctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE is_active = 1";

        try (Connection conn = DatabaseManager.getInstance().getConnection();) {
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
            ServerLogger.error("Error fetching auctions: " + e.getMessage());
        }
        return activeAuctions;
    }

    private void endAuction(int auctionId) {
        Auction auction = activeAuctions.remove(auctionId);
        if (auction != null) {
            ServerLogger.info("Auction " + auctionId + " has officially ended!");

            // 1. Change status to CLOSED or COMPLETED in your DB via AuctionDAO

            // 2. Broadcast a "AUCTION_ENDED" notification to all connected clients via ServerApp.broadcast()
        }
    }

}

