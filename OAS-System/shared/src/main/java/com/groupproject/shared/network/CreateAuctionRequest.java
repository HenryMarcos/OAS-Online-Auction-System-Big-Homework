package com.groupproject.shared.network;

import java.util.HashMap;
import java.util.Map;

import com.groupproject.shared.model.categories.Category;

public class CreateAuctionRequest extends Request {
    private int sellerId;
    private String title;
    private String description;
    private Category category;
    Map<Integer, Map<String, String>> categoryGroupedSpecs = new HashMap<>();
    private double startingPrice;
    private String endTime;

    public CreateAuctionRequest(int sellerId, String title, String description, 
                                Category category, double startingPrice, String endTime) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.endTime = endTime;
    }

    public int getSelletId() { return sellerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public double getStartingPrice() { return startingPrice; }
    public String getEndTime() { return endTime; }
}
