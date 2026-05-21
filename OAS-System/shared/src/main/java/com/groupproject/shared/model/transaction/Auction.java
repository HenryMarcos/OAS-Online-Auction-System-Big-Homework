package com.groupproject.shared.model.transaction;

import java.time.LocalDateTime;
import java.util.Map;

import com.groupproject.shared.model.base.Entity;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;

public class Auction extends Entity {
    private static final long serialVersionUID = 1L;

    private int sellerId; // ID của người bán
    private String title; // Tên sản phẩm
    private String description; // Mô tả sản phẩm
    private Category category; // Danh mục sản phẩm
    private double startingPrice;
    Map<Integer, Map<String, String>> categoryGroupedSpecs;
    private double currentBid; // Giá hiện tại của sản phẩm trong phiên đấu giá
    private Integer highestBidderId; // ID của người đang có giá cao nhất
    private LocalDateTime endTime; // Ngày kết thúc của phiên đấu giá
    private AuctionStatus status; // Trạng thái của phiên đấu giá (ví dụ: "active", "closed", "cancelled", ...)

    public Auction( int sellerId, String title, String desciption, Category category, 
                   Map<Integer, Map<String, String>> categoryGroupedSpecs, double startingPrice, LocalDateTime endTime) {
        super();
        this.sellerId = sellerId;
        this.title = title;
        this.description = desciption;
        this.category = category;
        this.categoryGroupedSpecs = categoryGroupedSpecs;
        this.startingPrice = startingPrice;
        this.endTime = endTime;
        this.status = AuctionStatus.WAITING;
    }

    public int getSellerId() { return sellerId; }

    public String getTitle() { return title;}

    public String getDescription() { return description; }

    public Category getCategory() { return category; }

    public Map<Integer, Map<String, String>> getCategoryGroupedSpecs() { return categoryGroupedSpecs; }

    public double getCurrentBid() { return currentBid; }
    public void setCurrentBid(double currentBid) { this.currentBid = currentBid; }

    public Integer getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(Integer highestBidderId) { this.highestBidderId = highestBidderId; }

    public double getStartingPrice() { return startingPrice; }

    public LocalDateTime getEndTime() { return endTime; }

    public AuctionStatus getStatus() { return status; }

    public void setStatus(AuctionStatus status) { this.status = status; }
}
