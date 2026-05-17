package com.groupproject.shared.model.transaction;
import java.time.LocalDateTime;

import com.groupproject.shared.model.base.Entity;
import com.groupproject.shared.model.enums.AuctionStatus;

public class AuctionItem extends Entity {
     private static final long serialVersionUID = 1L;
    private Integer id; // id của phiên đấu giá 
    private int sellerId; // id của người tạo ra phiên đấu giá(basic)
    private String itemName;
    private double currentBid;
    private String highestBidderName;
    private Integer highestBidderId; // người có giá đặt cao nhất
    private AuctionStatus status;
    private LocalDateTime endDate;
    public AuctionItem() {
        super();
        itemName="";
        //this.sellerId = null;
        this.currentBid = 0.0;
        this.highestBidderName = "";
        this.highestBidderId=null;
        this.status = AuctionStatus.WAITING;
    }
    public AuctionItem(String itemName, int sellerId, double currentBid, String highestBidderName,Integer highestBidderId,LocalDateTime endDate) {
        super();
        //this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentBid = currentBid;
        this.highestBidderName= this.highestBidderName;
        this.highestBidderId= this.highestBidderId;
        this.endDate = endDate;
        this.status = AuctionStatus.WAITING;
    }
    public int getSellerId() {
        return sellerId;
    }
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName= itemName;
    }
    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }

    public Integer getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(Integer highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public AuctionStatus getAuctionStatus() {
        return status;
    }
    public void setAuctionStatus(AuctionStatus status) {
        this.status = status;
    }
}
