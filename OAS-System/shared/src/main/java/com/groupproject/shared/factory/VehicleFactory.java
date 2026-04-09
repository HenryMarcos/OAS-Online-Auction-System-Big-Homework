package com.groupproject.shared.factory;

import com.groupproject.shared.model.item.Item; 
import com.groupproject.shared.model.item.Vehicle;

// Thiết kế một class VehicleFactory để tạo đối tượng Vehicle, nó sẽ triển khai interface ItemCreator
public class VehicleFactory implements ItemCreator {
    @Override
    public Item createItem(String name, double basePrice, String sellerId, String description, String... attributes) {
        // Kiểm tra nếu không có đủ thuộc tính cần thiết để tạo đối tượng Vehicle
        if (attributes == null || attributes.length < 2) {
            throw new IllegalArgumentException("Phải có hai thuộc tính để tạo đối tượng Vehicle: brand và model");
        }
        return new Vehicle(name, basePrice, sellerId, description, attributes[0], attributes[1]);
    }
}
