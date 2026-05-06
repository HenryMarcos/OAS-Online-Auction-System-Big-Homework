package com.groupproject.shared.factory;

import java.util.HashMap;
import java.util.Map;

import com.groupproject.shared.model.item.Item;

public class ItemFactory {
    private static final Map<String, ItemCreator> creators = new HashMap<>();

    static {
        creators.put("art", new ArtFactory());
        creators.put("electronic", new ElectronicFactory());
        creators.put("vehicle", new VehicleFactory());
    }

    public static Item createItem(String type, String name, String description, String... attributes) {
        ItemCreator creator = creators.get(type.trim().toLowerCase());
        if (creator == null) {
            throw new IllegalArgumentException("Loại sản phẩm không hợp lệ: " + type);
        }
        return creator.createItem(name, description, attributes);
    }
}
