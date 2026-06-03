package com.groupproject.shared.model.item;

import java.util.HashMap;
import java.util.Map;

import com.groupproject.shared.model.base.Entity;

public class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String title; // Tên sản phẩm
    private String description; // Mô tả chi tiết về sản phẩm, có thể bao gồm thông tin về tình trạng, kích thước, màu sắc, v.v.
    //private double startingPrice; // Giá ban đầu khi được mang đi đấu giá
    //private String sellerId; // ID của người bán
    // Không trỏ trực tiếp tham chiếu sellerId đến đối tượng Seller để tránh việc truyền quá nhiều thông tin không cần thiết qua mạng lưới, chỉ cần lưu ID là đủ để xác định người bán khi cần thiết
    // Nếu cần có thể sửa lại để biến sellerId thành một đối tượng Seller, nhưng cần đảm bảo rằng khi truyền qua mạng lưới thì chỉ truyền ID của người bán thay vì toàn bộ thông tin của đối tượng Seller để tối ưu hóa hiệu suất và bảo mật thông tin

    private int categoryId;
    private String categoryName;

    private Map<String, String> categorySpecificFields;

    public Item(int id, String title, String description, int categoryId, String categoryName) {
        super(id); 
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        //this.sellerId = sellerId;

        this.categorySpecificFields = new HashMap<>();
    }

    public void setTitle(String title) { this.title = title; }
    public String getTitle() { return title; }

    public void setDescription(String description) { this.description = description; }
    public String getDescription() { return description; }

    /*
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    public String getSellerId() {
        return sellerId;
    }
    */

    public void addSpecificField(String fieldName, String value) {
        this.categorySpecificFields.put(fieldName, value);
    }

    public String getSpecificField(String fieldName) {
        return this.categorySpecificFields.get(fieldName);
    }

    public Map<String, String> getAllSpecificFields() {
        return categorySpecificFields;
    }
}
