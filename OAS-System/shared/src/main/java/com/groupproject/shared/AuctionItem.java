package com.groupproject.shared;

import java.io.Serializable;

public class AuctionItem implements Serializable {
    private int id;
    private String itemName;
    private Double currentBid;
    private String highestBid;

    public AuctionItem(int id, String itemName, Double currentBid, String highestBid) {
        this.id = id;
        this.itemName = itemName;
        this.currentBid = currentBid;
        this.highestBid = highestBid;
    }

    public int getId() { return id; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemName() {return itemName; }
    public void setcurrentBid(Double currentBid) { this.currentBid = currentBid; }
    public Double getcurrentBid() {return currentBid; }
    public void sethighestBid(String highestBid) { this.highestBid = highestBid; }
    public String gethighestBid() {return highestBid; }
    
}
