package com.groupproject.shared.model.item;

import com.groupproject.shared.model.base.Entity;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String name; // Tên sản phẩm
    private String description; // Mô tả chi tiết về sản phẩm, có thể bao gồm thông tin về tình trạng, kích thước, màu sắc, v.v.
    private double basePrice; // Giá ban đầu khi được mang đi đấu giá
    private String sellerId; // ID của người bán
    //Không trỏ trực tiếp tham chiếu sellerId đến đối tượng Seller để tránh việc truyền quá nhiều thông tin không cần thiết qua mạng lưới, chỉ cần lưu ID là đủ để xác định người bán khi cần thiết
    //Nếu cần có thể sửa lại để biến sellerId thành một đối tượng Seller, nhưng cần đảm bảo rằng khi truyền qua mạng lưới thì chỉ truyền ID của người bán thay vì toàn bộ thông tin của đối tượng Seller để tối ưu hóa hiệu suất và bảo mật thông tin

    public Item() {
        super();
        this.name = "";
        this.description = "";
        this.basePrice = 0.0;
        this.sellerId = "";
    }

    public Item(String name, double basePrice, String sellerId, String description) {
        super(); 
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.sellerId = sellerId;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerId() {
        return sellerId;
    }
}
