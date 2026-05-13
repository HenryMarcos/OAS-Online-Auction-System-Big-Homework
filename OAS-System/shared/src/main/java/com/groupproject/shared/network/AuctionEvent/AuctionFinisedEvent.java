package com.groupproject.shared.network.AuctionEvent;

public class AuctionFinisedEvent extends AuctionEvent {
    private final double finalPrice;
    private final int winnerId;
    public AuctionFinisedEvent(int auctionId, int winnerId, double finalPrice) {
        super(auctionId);
        this.winnerId= winnerId;
        this.finalPrice= finalPrice;
    }
}
