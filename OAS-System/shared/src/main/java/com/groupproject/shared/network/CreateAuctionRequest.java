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
    private String startTime;
    private String endTime;
    private AuctionStatus status;

    /* TODO: Cần xác định rõ logic về startTime và status khi tạo đấu giá mới trong client */
    // Nếu người dùng chọn "Lên lịch đấu giá" thì startTime sẽ có giá trị, status sẽ là SHEDULED
    // Nếu người dùng không chọn thì startTime sẽ là null, status sẽ là WAITING
    public CreateAuctionRequest(int sellerId, String title, String description, Category category, 
                                Map<Integer, Map<String, String>> categoryGroupedSpecs, double startingPrice, String startTime, String endTime, AuctionStatus status) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.categoryGroupedSpecs = categoryGroupedSpecs;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }
    // Electronics: {condition: New, brand: Dell}
    // Laptop: {cpu: Intel Core i7, ram: 16GB, storage: 512GB SSD}

    public int getSellerId() { return sellerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Map<Integer, Map<String, String>> getCategoryGroupedSpecs() { return categoryGroupedSpecs; }
    public double getStartingPrice() { return startingPrice; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
}
