package com.groupproject.shared;

public class Electronics extends Item {

    // Các thuộc tính riêng biệt chỉ có ở đồ điện tử (giữ nguyên như cũ)
    private String brand;
    private int warrantyMonths;

    // CẬP NHẬT CONSTRUCTOR: Khớp với 3 thuộc tính mới của lớp Item
    public Electronics(float startingPrice, String itemName, String description, String brand, int warrantyMonths) {
        
        // Truyền 3 tham số lên cho lớp cha Item xử lý
        super(startingPrice, itemName, description); 
        
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    // --- Getters và Setters cho các thuộc tính riêng ---

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
}