package com.groupproject.client.Data;
import java.time.*;
public class Item {
    private String name;
    private String category;
    private String imagePath;
    private LocalDateTime enddate;
    private double startprice;
    private String description;
    public Item(String name, String category, double startprice,LocalDateTime enddate, String description ,String imagePath) {
        this.name= name;
        this.category=category;
        this.imagePath=imagePath;
        this.startprice= startprice;
        this.description=description;
        this.enddate= enddate;
    }
    public void setName(String name) {
        this.name= name;
    }
    public void setCategory(String category) {
        this.category= category;
    }
    public void setStartingPrice(double startprice) {
        this.startprice= startprice;
    }
    public void setEndDate(LocalDateTime enddate) {
        this.enddate= enddate;
    }
    public void setDescription(String description) {
        this.description= description;
    }
    public void setImagePath(String imagePath) {
        this.imagePath= imagePath;
    }
    public String getName() {
        return name;
    }
    public String getCategory() {
        return category;
    }
    public double getStartingPrice() {
        return startprice;
    }
    public LocalDateTime getEndDate() {
        return enddate;
    }
    public String getDescription() {
        return description;
    }
    public String getImagePath() {
        return imagePath;
    }
    
}
