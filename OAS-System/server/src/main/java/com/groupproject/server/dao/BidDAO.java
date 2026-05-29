package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.transaction.BidDTO;
import com.groupproject.shared.network.requests.PlaceBidRequest;

public class BidDAO {

    // Returns true if successful, false if something failed
    public static boolean insertBid(int auctionId, int bidderId, double amount) {
        String sql = "INSERT INTO bids (auction_id, bidder_id, amount, bid_time) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            pstmt.setInt(2, bidderId);
            pstmt.setDouble(3, amount);
            pstmt.setString(4, LocalDateTime.now().toString());
            
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

    public static boolean insertBid(PlaceBidRequest request, ClientHandler clientContext) {
        return insertBid(request.getAuctionId(), clientContext.getAuthenticatedUserId(), request.getBidAmount());
    }

    private static void updateAuctionPrice(Connection conn, int auctionId, double newPrice) throws Exception {
        String sql = "UPDATE auctions SET current_bid = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, auctionId);
            pstmt.executeUpdate();
        }
    }

    public static List<BidDTO> getBidsForAuction(int auctionId) {
        List<BidDTO> pastBids = new ArrayList<>();
        // ASC ensures oldest bids are first (perfect for graph drawing)
        String sql = "SELECT bidder_id, amount, bid_time FROM bids WHERE auction_id = ? ORDER BY bid_time ASC";
        
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int bidderId = rs.getInt("bidder_id");
                    double amount = rs.getDouble("amount");
                    String timeStr = rs.getString("bid_time");
                    
                    LocalDateTime bidTime = timeStr != null ? LocalDateTime.parse(timeStr) : LocalDateTime.now();
                    pastBids.add(new BidDTO("User " + bidderId, amount, bidTime));
                }
            }
        } catch (Exception e) {
            ServerLogger.error("Failed to fetch historical bids: " + e.getMessage());
        }
        return pastBids;
    }

    public static List<Integer> getUniqueBidders(int auctionId) {
        List<Integer> bidderIds = new ArrayList<>();
        String sql = "SELECT DISTINCT bidder_id FROM bids WHERE auction_id = ?";
        
        try (java.sql.Connection conn = DatabaseManager.INSTANCE.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    bidderIds.add(rs.getInt("bidder_id"));
                }
            }
        } catch (Exception e) {
            ServerLogger.error("Failed to fetch unique bidders: " + e.getMessage());
        }
        return bidderIds;
    }
}
