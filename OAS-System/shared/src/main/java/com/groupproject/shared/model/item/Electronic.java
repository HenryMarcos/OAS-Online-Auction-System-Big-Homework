package com.groupproject.shared.model.item;

public class Electronic extends Item {
    private static final long serialVersionUID = 1L;

    private String brand; // Thương hiệu của sản phẩm điện tử
    private String model; // Mẫu mã của sản phẩm điện tử

    public Electronic() {
        super();
    }

    public Electronic(String name, double basePrice, String sellerId, String description, String brand, String model) {
        super(name, basePrice, sellerId, description);
        this.brand = brand;
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
