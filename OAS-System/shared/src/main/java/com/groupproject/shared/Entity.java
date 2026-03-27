// Lớp cơ sở cho các lớp khác kế thừa
package com.groupproject.shared;

import java.io.Serializable;
import java.util.UUID;

// Cần có Serializable để có thể truyền thông tin qua mạng lưới
public abstract class Entity implements Serializable {
    
    // Giúp Java xác minh phiên bản của đối tượng khi nó tới mạng lưới
    private static final long serialVersionUID = 1L;

    private String id;

    public Entity() {
        // Tự động khởi tạo 1 id mới độc nhất mỗi khi tạo ra 1 Entity
        this.id = UUID.randomUUID().toString();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
