package com.groupproject.shared.network.AuctionEvent;

public class AuctionCancelledEvent extends AuctionEvent {
    private final String reason ;
    public AuctionCancelledEvent (int auctionId, String reason) {
        super(auctionId);
        this.reason = reason;
    }
    public String getReason() {
        return reason;
    }
}
