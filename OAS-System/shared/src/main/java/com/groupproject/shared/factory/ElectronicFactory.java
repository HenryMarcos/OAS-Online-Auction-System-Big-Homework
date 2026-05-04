package com.groupproject.shared.factory;

import com.groupproject.shared.model.item.Electronic;
import com.groupproject.shared.model.item.Item;

// Thiết kế một class ElectronicFactory để tạo đối tượng Electronic, nó sẽ triển khai interface ItemCreator
public class ElectronicFactory implements ItemCreator {
    @Override
    public Item createItem(String name, double basePrice, String sellerId, String description, String... attributes) {
        // Kiểm tra nếu không có đủ thuộc tính cần thiết để tạo đối tượng Electronic
        if (attributes == null || attributes.length < 2) {
            throw new IllegalArgumentException("Phải có hai thuộc tính để tạo đối tượng Electronic: brand và model");
        }
        return new Electronic(name, basePrice, sellerId, description, attributes[0], attributes[1]);
    }
}
