package com.groupproject.shared;

import java.io.Serializable;

public class AuthRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type; // LOGIN hoặc SIGNUP
    private String username; // Tên người dùng
    private String email; // Email, có thể để trống
    private String password; // Mật khẩu

    // Constructor cho login
    public AuthRequest(String type, String username, String email, String password) {
        this.type = type;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Constructor cho signup
    public AuthRequest(String type, String username, String password) {
        this(type, username, null, password);
    }

    // Getters
    public String getType() { return type; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }

}
