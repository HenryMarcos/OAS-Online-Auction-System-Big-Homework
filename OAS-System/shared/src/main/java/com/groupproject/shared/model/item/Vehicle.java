package com.groupproject.shared.model.item;

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;

    private String brand; // Hãng sản xuất của xe
    private String model; // Mẫu mã của xe

    public Vehicle() {
        super();
    }

    public Vehicle(String name, String description, String brand, String model) {
        super(name, description);
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
