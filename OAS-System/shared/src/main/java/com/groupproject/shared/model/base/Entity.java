package com.groupproject.shared.model.base;

import java.io.Serializable;
import java.time.LocalDateTime;

// Cần có Serializable để có thể truyền thông tin qua mạng lưới
public abstract class Entity implements Serializable {
    // Giúp Java xác minh phiên bản của đối tượng khi nó tới mạng lưới
    private static final long serialVersionUID = 1L;

    private int id; // Mỗi Entity sẽ có một id duy nhất được tạo tự động khi khởi tạo đối tượng
    private LocalDateTime createdAt; // Lưu thời gian tạo đối tượng, có thể hữu ích cho việc quản lý dữ liệu sau này

    public Entity(int id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    } 

    public Entity(int id) {
        this(id, LocalDateTime.now());
    }

    public void setId(int id) {this.id = id;}
    public int getId() {return id;}

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCreatedAt() {return createdAt;}
}
