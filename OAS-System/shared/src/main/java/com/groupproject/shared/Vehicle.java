package com.groupproject.shared;

public class Vehicle extends Item {

    // Các thuộc tính đặc trưng của phương tiện
    private String brand;   
    private int mileage;    

    public Vehicle(float startingPrice, String itemName, String description, String brand, int mileage) {
        
        // Truyền 3 tham số lên cho lớp cha Item xử lý
        super(startingPrice, itemName, description);
        
        this.brand = brand;
        this.mileage = mileage;
    }

    // --- Getters / Setters cho các thuộc tính riêng ---

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }
}