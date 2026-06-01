package com.groupproject.shared.network.AuctionEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BidUpdatedEvent extends AuctionEvent {
    private final double newBidAmount;
    private final int  highestBidderId;
    private final String timeStamp;
    public BidUpdatedEvent(int auctionId,double newBidAmount, int highestBidderId) {
        super(auctionId);
        this.newBidAmount= newBidAmount;
        this.highestBidderId = highestBidderId ;
        this.timeStamp= LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    public double getBidAmount() {
        return newBidAmount;
    }
    public int getHighestBidderId() {
        return highestBidderId;
    }
    public String getTimeStamp() {
        return timeStamp;
    }
    @Override 
    public void accept(AuctionListener listener) {
        listener.onBidUpdated(this);
    }
}
