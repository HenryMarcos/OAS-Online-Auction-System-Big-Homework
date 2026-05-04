package com.groupproject.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.groupproject.shared.AuctionItem;
import com.groupproject.shared.BidRequest;

public class AuctionManager {
    private static final String DB_URL = "jdbc:sqlite:users.db";

    public static synchronized boolean proccessBid(int auctionId, String bidderUsername, double bidAmount) {
        String checkSql = "SELECT current_bid, is_active FROM auctions WHERE id = ?";
        String updateSql = "UPDATE auctions SET current_bid = ?, highest_bidder = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL)) {

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

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
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
