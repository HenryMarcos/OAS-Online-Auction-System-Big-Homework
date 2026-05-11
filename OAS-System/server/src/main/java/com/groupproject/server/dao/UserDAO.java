package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.LoginRequest;
import com.groupproject.shared.network.SignupRequest;

public class UserDAO {

    public static synchronized String checkDuplicates(String username, String email) {
        String sql = "SELECT username, email FROM users WHERE username = ? OR email = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next() /* Có user trùng dữ liệu */) {
                String foundUsername = rs.getString("username");
                String foundEmail = rs.getString("email");

                boolean usernameMatch = foundUsername.equalsIgnoreCase(username);
                boolean emailMatch = foundEmail.equalsIgnoreCase(email);

                if (usernameMatch && emailMatch) {
                    System.out.println("Username and email already exists.");
                    return "Username and email already exists.";
                } else if (usernameMatch) {
                    System.out.println("Username is already exists");
                    return "Username is already exists";
                } else if (emailMatch) {
                    System.out.println("An account with that email already exists.");
                    return "An account with that email already exists.";
                }
            }
            
        } 
        catch (SQLException e) {} 
        catch (Exception e) {}

        return null; // Trả về null tức là không có tài khoản nào trùng cả
    }
    public static synchronized String checkDuplicates(SignupRequest request) {
        return checkDuplicates(request.getUsername(), request.getEmail());
    }

    public static synchronized User registerUser(String username, String email, String password) {
        // Câu lệnh sql để chèn user mới
        String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.executeUpdate(); // Chạy câu lệnh

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int newId = rs.getInt(1); // 1 thay vì "id", vì rs không lấy tên cột
                return new User(newId, username, password, email); // Đăng ký thành công
            } else {
                System.err.println("Error: Can't get user's id for some reason");
            }
        } catch (SQLException e) {
            System.err.println("Database error during registration: " + e.getMessage());
        }
        return null; // Lưu thất bại
    }

    public static synchronized User registerUser(SignupRequest request) {
        return registerUser(request.getUsername(), request.getEmail(), request.getPassword());
    }

    public static synchronized boolean checkUser(String username, String password) {
        // Câu lệnh SQL tìm user có username và password khớp
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            
            // Nếu rs.next() là true, nghĩa là tìm thấy ít nhất 1 dòng khớp -> Đăng nhập thành công
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized boolean checkUser(LoginRequest request) {
        return checkUser(request.getUsername(), request.getPassword());
    }
}
