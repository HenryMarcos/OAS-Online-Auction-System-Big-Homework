package com.groupproject.shared.network.event;

public class AuctionStartedEvent extends ServerEvent {
    private int auctionId;

    public AuctionStartedEvent(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() { return auctionId; }
}