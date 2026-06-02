package com.groupproject.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.requests.LoginRequest;
import com.groupproject.shared.network.requests.SignupRequest;

public class UserDAO {

    // --- KIỂM TRA QUYỀN ADMIN TỪ BẢNG admin_list ---
    public static synchronized boolean isAdmin(int userId) {
        String sql = "SELECT 1 FROM admin_list WHERE user_id = ?";
        
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            return rs.next(); // Nếu có kết quả tức là user này nằm trong bảng admin
        } catch (Exception e) {
            System.err.println("UserDAO:isAdmin: " + e.getMessage());
            return false;
        }
    }

    public static synchronized String checkDuplicates(String username, String email) {
        String sql = "SELECT username, email FROM users WHERE username = ? OR email = ?";
        

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next() /* Có user trùng dữ liệu */) {
                String foundUsername = rs.getString("username");
                String foundEmail = rs.getString("email");

                boolean usernameMatch = foundUsername.equalsIgnoreCase(username);
                boolean emailMatch = foundEmail.equalsIgnoreCase(email);

                if (usernameMatch && emailMatch) {
                    ServerLogger.info("Username and email already exists.");
                    return "Username and email already exists.";
                } else if (usernameMatch) {
                    ServerLogger.info("Username is already exists");
                    return "Username is already exists";
                } else if (emailMatch) {
                    ServerLogger.info("An account with that email already exists.");
                    return "An account with that email already exists.";
                }
            }
            
        } catch (SQLException e) {
            ServerLogger.error("UserDAO:checkDuplicates: " + e.getMessage());
        } catch (Exception e) {
            ServerLogger.error("UserDAO:checkDuplicates: " + e.getMessage());
        }

        return null; // Trả về null tức là không có tài khoản nào trùng cả
    }

    public static synchronized String checkDuplicates(SignupRequest request) {
        return checkDuplicates(request.getUsername(), request.getEmail());
    }

    public static synchronized User registerUser(String username, String email, String password) {
        // Câu lệnh sql để chèn user mới
        String sql = "INSERT INTO users (username, email, password, created_at) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            LocalDateTime noww = LocalDateTime.now();
            pstmt.setObject(4, noww);
            pstmt.executeUpdate(); // Chạy câu lệnh

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int newId = rs.getInt(1); // 1 thay vì "id", vì rs không lấy tên cột
                return new User(newId, username, password, email, 10000, noww); // Đăng ký thành công
            } else {
                System.err.println("Error: Can't get user's id for some reason");
            }
        } catch (SQLException e) {
            ServerLogger.error("UserDAO:registerUser: " + e.getMessage());
        }
        return null; // Lưu thất bại
    }

    public static synchronized User registerUser(SignupRequest request) {
        return registerUser(request.getUsername(), request.getEmail(), request.getPassword());
    }

    public static synchronized boolean checkUser(String username, String password) {
        // Câu lệnh SQL tìm user có username và password khớp
        String sql = "SELECT 1 FROM users WHERE username = ? AND password = ?";
        

        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            
            // Nếu rs.next() là true, nghĩa là tìm thấy ít nhất 1 dòng khớp -> Đăng nhập thành công
            return rs.next();
        } catch (Exception e) {
            ServerLogger.error("UserDAO:checkUser: " + e.getMessage());
            return false;
        }
    }

    public static synchronized boolean checkUser(LoginRequest request) {
        return checkUser(request.getUsername(), request.getPassword());
    }

    public static synchronized User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                ServerLogger.info("Found user with the same detail");
                // username và password đã có sẵn
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String email = rs.getString("email");
                String password = rs.getString("password");
                double balance = rs.getDouble("balance");
                String createdAtStr = rs.getString("created_at");
                LocalDateTime createdAt = (createdAtStr != null) ? LocalDateTime.parse(createdAtStr.replace(" ", "T")) : LocalDateTime.now();
                
                // KIỂM TRA QUYỀN ADMIN
                boolean isUserAdmin = isAdmin(id);

                return new User(id, username, password, email, balance, createdAt);
            }
        } catch (Exception e) {
            ServerLogger.error("UserDAO:getUserById: " + e.getMessage());
        }
        return null;
    }

    public static synchronized boolean updateBalance(int userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, newBalance);
            pstmt.setInt(2, userId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            ServerLogger.error("UserDAO:updateBalance: " + e.getMessage());
            return false;
        }
    }

    public static synchronized User getUser(String username, String password) {
        ServerLogger.info(String.format("Getting user by username: %s and password: %s", username, password));
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        
        
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            
            // Nếu rs.next() là true, nghĩa là tìm thấy ít nhất 1 dòng khớp -> Đăng nhập thành công
            if (rs.next()) {
                ServerLogger.info("Found user with the same detail");
                // username và password đã có sẵn
                int id = rs.getInt("id");
                String email = rs.getString("email");
                double balance = rs.getDouble("balance");
                String createdAtStr = rs.getString("created_at");
                LocalDateTime createdAt = (createdAtStr != null) ? LocalDateTime.parse(createdAtStr.replace(" ", "T")) : LocalDateTime.now();
                
                // KIỂM TRA QUYỀN ADMIN
                boolean isUserAdmin = isAdmin(id);

                return new User(id, username, password, email, 10000, createdAt);
            }
        } catch (Exception e) {
            ServerLogger.error("UserDAO:getUser: " + e.getMessage());
            return null;
        }

        return null;
    }
    
    public static synchronized User getUser(LoginRequest request) {
        return getUser(request.getUsername(), request.getPassword());
    }

    public static synchronized User getUser(SignupRequest request) {
        return getUser(request.getUsername(), request.getPassword());
    }

    public static boolean addBalance(int userId, double amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE id = ?";
        try (java.sql.Connection conn = DatabaseManager.INSTANCE.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, amount);
            pstmt.setInt(2, userId);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (java.sql.SQLException e) {
            ServerLogger.error("Lỗi cập nhật số dư khi nạp tiền: " + e.getMessage());
            return false;
        }
    }

    public static double getBalance(int userId) {
        String sql = "SELECT balance FROM users WHERE id = ?";
        try (java.sql.Connection conn = DatabaseManager.INSTANCE.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            java.sql.ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("balance");
        } catch (java.sql.SQLException e) {
            ServerLogger.error("UserDAO:getBalance: " + e.getMessage());
        }
        return -1; // sentinel: lỗi không lấy được số dư
    }
}