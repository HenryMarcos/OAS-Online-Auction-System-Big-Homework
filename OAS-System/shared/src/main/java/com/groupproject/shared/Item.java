package com.groupproject.shared;

public class Item extends Entity {
    
    private float startingPrice; 
    private String itemName;     
    private String description;  

    // Constructor khởi tạo
    public Item(float startingPrice, String itemName, String description) {
        super(); // Gọi lên Entity để tự động sinh ra một mã ID duy nhất
        this.startingPrice = startingPrice;
        this.itemName = itemName;
        this.description = description;
    }

    // 3. Getters & Setters
    public float getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(float startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}