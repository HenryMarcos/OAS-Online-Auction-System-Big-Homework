package com.groupproject.shared.model.user;

import java.time.LocalDateTime;

import com.groupproject.shared.model.base.Entity;

public class User extends Entity {

    private String username;
    private String password; // Mật khẩu sẽ được mã hóa trước khi lưu trữ hoặc truyền qua mạng
    private String email;
    private double accountBalance;

    public User(int id, String username, String password, String email, double accountBalance, LocalDateTime createdAt) {
        super(id, createdAt); 
        this.username = username;
        this.password = password;
        this.email = email;
        this.accountBalance = accountBalance;
    }

    public void setUsername(String username) { this.username = username; }
    public String getUsername() { return username; }

    public void setPassword(String password) { this.password = password; }
    public String getPassword() { return password; }

    public void setEmail(String email) { this.email = email; }
    public String getEmail() { return email; }

    public void setAccountBalance(double accountBalance) { this.accountBalance = accountBalance; }
    public double getAccountBalance() { return accountBalance; }
}
