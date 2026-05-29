package com.groupproject.shared.network.events;

public class NewBidEvent extends ServerEvent {
    private int auctionId;
    private double newBidAmount;
    private int highestBidderId;

    public NewBidEvent(int auctionId, double newBidAmount, int highestBidderId) {
        this.auctionId = auctionId;
        this.newBidAmount = newBidAmount;
        this.highestBidderId = highestBidderId;
    }

    public int getAuctionId() { return auctionId; }
    public double getNewBidAmount() { return newBidAmount; }
    public int getHighestBidderId() { return highestBidderId; }
}
