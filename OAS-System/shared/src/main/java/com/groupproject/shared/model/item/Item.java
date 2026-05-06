package com.groupproject.shared.model.item;

import com.groupproject.shared.model.base.Entity;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String name; // Tên sản phẩm
    private String description; // Mô tả chi tiết về sản phẩm, có thể bao gồm thông tin về tình trạng, kích thước, màu sắc, v.v.

    public Item() {
        super();
        this.name = "";
        this.description = "";
    }

    public Item(String name, String description) {
        super(); 
        this.name = name;
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
