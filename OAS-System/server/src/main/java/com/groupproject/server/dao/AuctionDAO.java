package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.CreateAuctionRequest;

public class AuctionDAO {
    public static synchronized Auction createAuction(int sellerId, String title, String description, 
                                                     Category category, double startingPrice, LocalDateTime endTime) {

        String sql = "INSERT INTO auction (sellerId, title, description, category_id, starting_price, end_time" +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = DatabaseManager.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sellerId);
            pstmt.setString(2, title);
            pstmt.setString(3, description);
            pstmt.setInt(4, category.getId());
            pstmt.setDouble(5, startingPrice);
            pstmt.setString(6, endTime.toString());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    return new Auction(newId, sellerId, title, description, category, startingPrice, endTime);
                }
            }
        } catch (SQLException e) {
            ServerLogger.error("Database error creating auction: " + e.getMessage());
        }
        return null;
    }

    public static synchronized Auction createAuction(CreateAuctionRequest request) {
        return createAuction(request.getSelletId(), request.getTitle(), request.getDescription(), 
                             request.getCategory(), request.getStartingPrice(), LocalDateTime.parse(request.getEndTime()));
    }


    public static List<Auction> getAuctions() {
        String sql = "SELECT * FROM auctions";

        Connection conn = DatabaseManager.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                int sellerId = rs.getInt("seller_id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                int categoryId = rs.getInt("category_id");
                double startingPrice = rs.getDouble("starting_price");
                String endTime = rs.getString("end_time");
                double currentBid = rs.getDouble("current_bid");
                Integer currentBidderId = rs.getInt("current_bidder_id");
                String status = rs.getString("status");

                Auction auction = new Auction(id, sellerId, title, description, null, startingPrice, null);
            }
            
        } catch (SQLException e) {
            ServerLogger.error("Database error getting auction: " + e.getMessage());
        }

        return null;
    }

}
