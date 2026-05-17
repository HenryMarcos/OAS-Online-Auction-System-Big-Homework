package com.groupproject.shared.network.AuctionEvent;

public class BidUpdatedEvent extends AuctionEvent {
    private final double newBidAmount;
    private final int  highestBidderId;
    public BidUpdatedEvent(int auctionId,double newBidAmount, int highestBidderId) {
        super(auctionId);
        this.newBidAmount= newBidAmount;
        this.highestBidderId = highestBidderId ;
    }
    public double getBidAmount() {
        return newBidAmount;
    }
    public int getHighestBidderId() {
        return highestBidderId;
    }
    @Override 
    public void accept(AuctionListener listener) {
        listener.onBidUpdated(this);
    }
}
