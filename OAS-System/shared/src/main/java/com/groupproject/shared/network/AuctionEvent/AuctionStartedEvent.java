package com.groupproject.shared.network.AuctionEvent;

public class AuctionStartedEvent extends AuctionEvent {
    public AuctionStartedEvent(int auctionId) {
        super(auctionId);
    }
    @Override 
    public void accept(AuctionListener listener) {
        listener.onAuctionStarted(this);
    }
}
