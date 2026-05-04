package com.groupproject.client.Data;
import java.time.*;
import java.time.format.DateTimeFormatter;
public class Item {
    private String name;
    private String category;
    private String imagePath;
    private LocalDateTime enddate;
    private double startprice;
    private double currentprice;
    public Item(String name, String category, double startprice,LocalDateTime enddate,String imagePath) {
        this.name= name;
        this.category=category;
        this.imagePath=imagePath;
        this.startprice= startprice;
        this.currentprice= startprice;
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
    public String getImagePath() {
        return imagePath;
    }
    public String getFormatEndDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return enddate.format(formatter);
    }
    public void setCurrentPrice(double currentprice) {
        this.currentprice= currentprice;
    }
    public double getCurrentPrice() {
        return currentprice;
    }
    
}
