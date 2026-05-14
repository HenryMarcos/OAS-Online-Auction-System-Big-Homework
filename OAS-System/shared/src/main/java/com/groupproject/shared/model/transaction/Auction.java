package com.groupproject.shared.model.transaction;

import java.time.LocalDateTime;

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
    private double currentBid; // Giá hiện tại của sản phẩm trong phiên đấu giá
    private String highestBidderId; // ID của người đang có giá cao nhất
    private LocalDateTime startDate; // Ngày bắt đầu của phiên đấu giá
    private LocalDateTime endDate; // Ngày kết thúc của phiên đấu giá
    private AuctionStatus status; // Trạng thái của phiên đấu giá (ví dụ: "active", "closed", "cancelled", ...)

    public Auction(int id, int sellerId, String title, String desciption, Category category, double startingPrice, LocalDateTime endDate) {
        super(id);
        this.sellerId = sellerId;
        this.title = title;
        this.description = desciption;
        this.category = category;
        this.startingPrice = startingPrice;
        this.startDate = LocalDateTime.now();
        this.endDate = endDate;
        this.status = AuctionStatus.WAITING;
    }

    public int getSellerId() { return sellerId; }

    public double getCurrentBid() { return currentBid; }
    public void setCurrentBid(double currentBid) { this.currentBid = currentBid; }

    public String getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }

    public double getStartingPrice() { return startingPrice; }

    public LocalDateTime getStartDate() { return startDate; }

    public LocalDateTime getEndDate() { return endDate; }

    public AuctionStatus getStatus() { return status; }

    public void setStatus(AuctionStatus status) { this.status = status; }
}
