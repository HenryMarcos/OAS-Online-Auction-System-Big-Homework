// Lớp cơ bản kế thừa từ lớp Entity, đại diện cho tất cả vật phẩm được mang đi đấu giá

package com.groupproject.shared;

public abstract class Item {
    
    private static final long serialVersionUID = 1L;

    private String name; // Tên sản phẩm
    private double basePrice; // Giá ban đầu khi được mang đi đấu giá

    public Item(String name, double basePrice) {
        super(); // Gọi constructor của lớp cha Entity, tạo 1 id ngẫu nhiên mới
        this.name = name;
        this.basePrice = basePrice;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getBasePrice() {
        return basePrice;
    }
}
