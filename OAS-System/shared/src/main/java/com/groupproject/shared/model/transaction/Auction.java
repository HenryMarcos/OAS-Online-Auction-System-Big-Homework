package com.groupproject.shared.model.transaction;

import java.time.LocalDateTime;

import com.groupproject.shared.model.base.Entity;
import com.groupproject.shared.model.enums.AuctionStatus;

public class Auction extends Entity {
    private static final long serialVersionUID = 1L;

    private Integer itemId; // ID của sản phẩm được đấu giá
    private String itemName; // Tên của sản phẩm được đấu giá
    private Integer sellerId; // ID của người bán
    private double currentBid; // Giá hiện tại của sản phẩm trong phiên đấu giá
    private String highestBidderId; // ID của người đang có giá cao nhất
    private LocalDateTime startDate; // Ngày bắt đầu của phiên đấu giá
    private LocalDateTime endDate; // Ngày kết thúc của phiên đấu giá
    private AuctionStatus status; // Trạng thái của phiên đấu giá (ví dụ: "active", "closed", "cancelled", ...)

    public Auction() {
        super();
        itemName="";
        this.sellerId = null;
        this.currentBid = 0.0;
        this.highestBidderId = "";
        this.status = AuctionStatus.WAITING;
    }
    public Auction(Integer itemId,String itemName, Integer sellerId, double currentBid, String highestBidderId) {
        super();
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentBid = currentBid;
        this.highestBidderId = highestBidderId;
        this.status = AuctionStatus.WAITING;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getSellerId() {
        return sellerId;
    }
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName= itemName;
    }
    public void setSellerId(Integer sellerId) {
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

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
}
