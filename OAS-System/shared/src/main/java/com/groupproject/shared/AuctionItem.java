package com.groupproject.shared;

import java.io.Serializable;

public class AuctionItem implements Serializable {
    private int id; // id của phiên đấu giá 
    private String itemName;
    private Double currentBid;
    private String highestBid; // người có giá đặt cao nhất

    public AuctionItem(int id, String itemName, Double currentBid, String highestBid) {
        this.id = id;
        this.itemName = itemName;
        this.currentBid = currentBid;
        this.highestBid = highestBid;
    }

    public int getId() { return id; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemName() {return itemName; }
    public void setCurrentBid(Double currentBid) { this.currentBid = currentBid; }
    public Double getCurrentBid() {return currentBid; }
    public void setHighestBid(String highestBid) { this.highestBid = highestBid; }
    public String getHighestBid() {return highestBid; }
    
}
