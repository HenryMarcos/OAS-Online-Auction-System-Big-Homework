package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.PlaceBidRequest;

public class BidDAO {
    // Returns true if successful, false if something failed
    public static boolean insertBid(int auctionId, int bidderId, double amount) {
        String sql = "INSERT INTO bids (auction_id, bidder_id, amount, bid_time) VALUES (?, ?, ?, NOW())";
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            pstmt.setInt(2, bidderId);
            pstmt.setDouble(3, amount);
            
            int rowsAffected = pstmt.executeUpdate();
            
            // Also, update the current_price in the auctions table!
            if (rowsAffected > 0) {
                updateAuctionPrice(conn, auctionId, amount);
                return true;
            }
        } catch (Exception e) {
            ServerLogger.error("Failed to insert bid: " + e.getMessage());
        }
        return false;
    }

    public static boolean insertBid(PlaceBidRequest request) {
        return insertBid(request.getAuctionId(), request.getBidderId(), request.getBidAmount());
    }

    private static void updateAuctionPrice(Connection conn, int auctionId, double newPrice) throws Exception {
        String sql = "UPDATE auctions SET current_bid = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, auctionId);
            pstmt.executeUpdate();
        }
    }
}
