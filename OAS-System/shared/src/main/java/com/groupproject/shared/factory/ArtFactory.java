package com.groupproject.shared.factory;

import com.groupproject.shared.model.item.Art;
import com.groupproject.shared.model.item.Item;

// Thiết kế một class ArtFactory để tạo đối tượng Art, nó sẽ triển khai interface ItemCreator
public class ArtFactory implements ItemCreator {
    @Override
    public Item createItem(String name, String description, String... attributes) {
        // Kiểm tra nếu không có đủ thuộc tính cần thiết để tạo đối tượng Art
        if (attributes == null || attributes.length < 1) {
            throw new IllegalArgumentException("Phải có một thuộc tính để tạo đối tượng Art: artist");
        }
        return new Art(name, description, attributes[0]);
    }
}
