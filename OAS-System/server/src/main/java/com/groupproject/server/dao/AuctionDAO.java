package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;

public class AuctionDAO {
    public static synchronized Auction createAuction(int sellerId, String title, String description, 
                                                     Category category, double startingPrice, LocalDateTime endTime) {

        String sql = "INSERT INO auction (sellerId, title, description, category_id, starting_price, end_time" +
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
            System.err.println("Database error creating auction: " + e.getMessage());
        }
        return null;
    }
}
