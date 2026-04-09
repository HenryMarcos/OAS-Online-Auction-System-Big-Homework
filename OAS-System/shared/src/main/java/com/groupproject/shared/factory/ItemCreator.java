package com.groupproject.shared.factory;

import com.groupproject.shared.model.item.Item;

// Thiết kế một interface ItemCreator để định nghĩa phương thức tạo đối tượng Item
interface ItemCreator {
    Item createItem(String name, double basePrice, String sellerId, String description, String... attributes);
}