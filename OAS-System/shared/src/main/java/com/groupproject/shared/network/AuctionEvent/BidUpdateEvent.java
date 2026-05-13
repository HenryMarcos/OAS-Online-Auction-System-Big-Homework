package com.groupproject.shared.network.AuctionEvent;

public class BidUpdateEvent extends AuctionEvent {
    private final double newBidAmount;
    private final int highestBidderId;
    public BidUpdateEvent(int auctionId,double newBidAmount, int highestBidderId) {
        super(auctionId);
        this.newBidAmount= newBidAmount;
        this.highestBidderId = highestBidderId;
    }
    public double getBidAmount() {
        return newBidAmount;
    }
    public int getHighestBidderName() {
        return highestBidderId;
    }
}
