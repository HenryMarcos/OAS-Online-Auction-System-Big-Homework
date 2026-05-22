package com.groupproject.shared.network;

import java.util.Map;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;

public class CreateAuctionRequest extends Request {
    private int sellerId;
    private String title;
    private String description;
    private Category category;
    Map<Integer, Map<String, String>> categoryGroupedSpecs;
    private double startingPrice;
    private String endTime;
    private AuctionStatus status;

    public CreateAuctionRequest(int sellerId, String title, String description, Category category, 
                                Map<Integer, Map<String, String>> categoryGroupedSpecs, double startingPrice, String endTime, AuctionStatus status) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.categoryGroupedSpecs = categoryGroupedSpecs;
        this.startingPrice = startingPrice;
        this.endTime = endTime;
        this.status = status;
    }

    public int getSellerId() { return sellerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Map<Integer, Map<String, String>> getCategoryGroupedSpecs() { return categoryGroupedSpecs; }
    public double getStartingPrice() { return startingPrice; }
    public String getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
}
