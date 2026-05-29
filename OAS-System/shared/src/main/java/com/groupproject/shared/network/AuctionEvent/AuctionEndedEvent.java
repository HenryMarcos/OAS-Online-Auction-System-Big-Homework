package com.groupproject.shared.network.AuctionEvent;

public class AuctionEndedEvent extends AuctionEvent {
    public AuctionEndedEvent(int auctionId) {
        super(auctionId);
    }
    @Override 
    public void accept(AuctionListener listener) {
        listener.onAuctionEnded(this);
    }
}
