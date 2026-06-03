package com.groupproject.shared.model.transaction;

import java.time.LocalDateTime;

import com.groupproject.shared.model.base.Entity;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;

public class Auction extends Entity {
    private static final long serialVersionUID = 1L;

    private int sellerId; 
    private String title; 
    
    // --- Legacy String paths ---
    private String mainImagePath;
    
    // 🌟 NEW: Byte Arrays for network transfer and image storage!
    private byte[] mainImageBytes;
    private Category category; 
    private double startingPrice;
    private double currentBid; 
    private Integer highestBidderId; 
    private long duration;
    private LocalDateTime startTime; 
    private LocalDateTime endTime; 
    private AuctionStatus status; 

    public Auction(int id, int sellerId, String title, byte[] mainImageBytes,
                   Category category,
                   double startingPrice, long duration, 
                   LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status) {
        super(id);
        this.sellerId = sellerId;
        this.title = title;
        this.mainImageBytes = mainImageBytes;
        this.category = category;
        this.startingPrice = startingPrice;
        this.duration = duration;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // --- NEW GETTERS AND SETTERS FOR BYTES ---
    public byte[] getMainImageBytes() { return mainImageBytes; }
    public void setMainImageBytes(byte[] mainImageBytes) { this.mainImageBytes = mainImageBytes; }

    // --- OLD GETTERS AND SETTERS ---
    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMainImagePath() { return mainImagePath; }
    public void setMainImagePath(String path) { this.mainImagePath = path; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public double getCurrentBid() { return currentBid; }
    public void setCurrentBid(double currentBid) { this.currentBid = currentBid; }

    public Integer getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(Integer highestBidderId) { this.highestBidderId = highestBidderId; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
}