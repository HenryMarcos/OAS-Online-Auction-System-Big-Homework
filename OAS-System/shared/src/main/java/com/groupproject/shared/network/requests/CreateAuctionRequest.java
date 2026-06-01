package com.groupproject.shared.network.requests;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;

public class CreateAuctionRequest extends Request {
    private String title;
    private String description;
    private Category category;
    Map<Integer, Map<String, String>> categoryGroupedSpecs;
    private byte[] mainImageBytes;
    private List<byte[]> subImagesBytes;
    private double startingPrice;
    private long duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    public CreateAuctionRequest(String title, String description, Category category, 
                                Map<Integer, Map<String, String>> categoryGroupedSpecs, 
                                byte[] mainImageBytes, List<byte[]> subImagesBytes,
                                double startingPrice, 
                                Long duration, LocalDateTime startTime, LocalDateTime endTime, 
                                AuctionStatus status) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.categoryGroupedSpecs = categoryGroupedSpecs;
        this.mainImageBytes = mainImageBytes;
        this.subImagesBytes = subImagesBytes;
        this.startingPrice = startingPrice;
        this.duration = duration != null ? duration : 0L;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }
    // Electronics: {condition: New, brand: Dell}
    // Laptop: {cpu: Intel Core i7, ram: 16GB, storage: 512GB SSD}

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Map<Integer, Map<String, String>> getCategoryGroupedSpecs() { return categoryGroupedSpecs; }
    public byte[] getMainImageBytes() { return mainImageBytes; }
    public List<byte[]> getSubImagesBytes() { return subImagesBytes; }
    public double getStartingPrice() { return startingPrice; }
    public long getDuration() { return duration; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
}
