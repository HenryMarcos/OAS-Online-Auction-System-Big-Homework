// Lớp kế thừa từ lớp Entity, là lớp cơ bản cho tất cả người dùng
package com.groupproject.shared;

public abstract class User extends Entity {
    
    private static final long serialVersionUID = 1L;

    private String username;
    private String email;

    public User(String username, String email) {
        super(); // Gọi constructor từ lớp cha Entity để tạo id ngẫu nhiên mới
        this.username = username;
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
