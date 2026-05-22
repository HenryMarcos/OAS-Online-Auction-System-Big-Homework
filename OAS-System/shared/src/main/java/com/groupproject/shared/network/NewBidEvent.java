package com.groupproject.shared.network;

public class NewBidEvent extends ServerEvent {
    private int auctionId;
    private double newHighestBid;

    public NewBidEvent(int auctionId, double newHighestBid) {
        this.auctionId = auctionId;
        this.newHighestBid = newHighestBid;
    }

    public int getAuctionId() { return auctionId; }
    public double getNewHighestBid() { return newHighestBid; } 
}
