package com.groupproject.client.Utlis;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.groupproject.client.Utlis.AuctionSession.PricePoint;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
// mô phỏng lại Item.java trong folder shared
// mo phong 
public class Item {
    private final ObservableList<PricePoint> priceHistory = FXCollections.observableArrayList();
    private String name;
    private String category;
    private String imagePath;
    private LocalDateTime enddate;
    private String description;
    private double startprice;
    private double currentprice;  // nên cho chạy theo real time 
    public Item(String name, String category, double startprice,LocalDateTime enddate,String description,String imagePath) {
        this.name= name;
        this.category=category;
        this.imagePath=imagePath;
        this.startprice= startprice;
        this.currentprice= startprice;
        this.description= description;
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
    public void setDescription(String description) {
        this.description = description;
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
    public String getDescription() {
        return description;
    }
    public void setCurrentPrice(double currentprice) {
        this.currentprice= currentprice;
    }
    public double getCurrentPrice() {
        return currentprice;
    }
    public ObservableList<PricePoint> getPriceHistory() { return priceHistory; }
    
}
