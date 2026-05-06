package com.groupproject.shared.model.user;

import com.groupproject.shared.model.base.Entity;

public class User extends Entity {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password; // Mật khẩu sẽ được mã hóa trước khi lưu trữ hoặc truyền qua mạng
    private String email;

    public User() {
        super();
        this.username = "";
        this.password = "";
        this.email = "";
    }

    public User(String username, String password, String email) {
        super(); 
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
