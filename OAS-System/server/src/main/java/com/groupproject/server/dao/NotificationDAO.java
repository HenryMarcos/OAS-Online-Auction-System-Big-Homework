package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.groupproject.server.utils.ServerLogger;

public class NotificationDAO {
    public static void createNotification(int userId, String message) {
        String sql = "INSERT INTO notifications (user_id, message) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
        } catch (Exception e) {
            ServerLogger.error("Failed to save notification: " + e.getMessage());
        }
    }
}
