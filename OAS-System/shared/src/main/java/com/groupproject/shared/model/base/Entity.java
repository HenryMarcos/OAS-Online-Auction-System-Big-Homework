package com.groupproject.shared.model.base;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

// Cần có Serializable để có thể truyền thông tin qua mạng lưới
public abstract class Entity implements Serializable {
    // Giúp Java xác minh phiên bản của đối tượng khi nó tới mạng lưới
    private static final long serialVersionUID = 1L;

    private String id; // Mỗi Entity sẽ có một id duy nhất được tạo tự động khi khởi tạo đối tượng
    private LocalDateTime createdAt; // Lưu thời gian tạo đối tượng, có thể hữu ích cho việc quản lý dữ liệu sau này

    public Entity() {
        // Tự động khởi tạo 1 id mới độc nhất mỗi khi tạo ra 1 Entity
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public void setId(String id) {this.id = id;}
    public String getId() {return id;}
    public LocalDateTime getCreatedAt() {return createdAt;}
}
