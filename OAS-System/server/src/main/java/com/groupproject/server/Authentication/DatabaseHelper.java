package com.groupproject.server.Authentication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.groupproject.shared.AuthRequest;

public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:users.db";

    public static synchronized boolean saveUser(String username, String email, String password) {
        // Câu lệnh sql để chèn user mới
        String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.executeUpdate(); // Chạy câu lệnh

            System.out.println("User signup success!");

            return true; // Đăng ký thành công

        } catch (Exception e) {
            // Sẽ văng lỗi nếu username đã tồn tại (do dính ràng buộc UNIQUE)
            return false;
        }
    }

    public static synchronized boolean saveUser(AuthRequest request) {
        return saveUser(request.getUsername(), request.getEmail(), request.getPassword());
    }

    public static synchronized boolean checkUser(String username, String password) {
        // Câu lệnh SQL tìm user có username và password khớp
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            
            // Nếu rs.next() là true, nghĩa là tìm thấy ít nhất 1 dòng khớp -> Đăng nhập thành công
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized boolean checkUser(AuthRequest request) {
        return checkUser(request.getUsername(), request.getPassword());
    }
}
