package com.groupproject.shared.model.transaction;

import java.time.LocalDate;

import com.groupproject.shared.model.base.Entity;
import com.groupproject.shared.model.enums.AuctionStatus;

public class Auction extends Entity {
    private static final long serialVersionUID = 1L;

    private String itemId; // ID của sản phẩm được đấu giá
    private String sellerId; // ID của người bán
    private double currentBid; // Giá hiện tại của sản phẩm trong phiên đấu giá
    private String highestBidderId; // ID của người đang có giá cao nhất
    private LocalDate startDate; // Ngày bắt đầu của phiên đấu giá
    private LocalDate endDate; // Ngày kết thúc của phiên đấu giá
    private AuctionStatus status; // Trạng thái của phiên đấu giá (ví dụ: "active", "closed", "cancelled", ...)

    public Auction() {
        super();
        this.itemId = "";
        this.sellerId = "";
        this.currentBid = 0.0;
        this.highestBidderId = "";
        this.status = AuctionStatus.WAITING;
    }

    public Auction(String itemId, String sellerId, double currentBid, String highestBidderId) {
        super();
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentBid = currentBid;
        this.highestBidderId = highestBidderId;
        this.status = AuctionStatus.WAITING;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
}
