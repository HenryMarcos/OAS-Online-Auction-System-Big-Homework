package com.groupproject.shared.model.user;

import java.time.LocalDateTime;

import com.groupproject.shared.model.base.Entity;

public class User extends Entity {

    private String username;
    private String password; 
    private String email;
    
    // CÁC THUỘC TÍNH MỚI THÊM
    private double balance;  // Số dư tài khoản (dùng double hoặc BigDecimal tùy bạn)
    private boolean isAdmin; // Cờ xác định quyền Admin (không lưu ở bảng users, nhưng gắn vào lúc runtime)

    /**
     * Constructor đầy đủ: Dùng khi load dữ liệu từ Database lên (lúc Đăng nhập)
     */
    public User(int id, String username, String password, String email, double balance, boolean isAdmin, LocalDateTime createdAt) {
        super(id, createdAt); 
        this.username = username;
        this.password = password;
        this.email = email;
        this.balance = balance;
        this.isAdmin = isAdmin;
    }

    /**
     * Constructor rút gọn: Dùng khi Đăng ký User mới (Mặc định tiền = 0, isAdmin = false)
     */
    public User(int id, String username, String password, String email, LocalDateTime createdAt) {
        super(id, createdAt); 
        this.username = username;
        this.password = password;
        this.email = email;
        this.balance = 0.0;     // Mặc định tài khoản mới có 0 đồng
        this.isAdmin = false;   // Mặc định tài khoản mới không phải admin
    }

    // --- GETTERS & SETTERS ---

    public void setUsername(String username) { this.username = username; }
    public String getUsername() { return username; }

    public void setPassword(String password) { this.password = password; }
    public String getPassword() { return password; }

    public void setEmail(String email) { this.email = email; }
    public String getEmail() { return email; }

    // Getter & Setter cho số dư
    public void setBalance(double balance) { this.balance = balance; }
    public double getBalance() { return balance; }

    // Getter & Setter cho quyền Admin
    public void setAdmin(boolean isAdmin) { this.isAdmin = isAdmin; }
    public boolean isAdmin() { return isAdmin; }
}