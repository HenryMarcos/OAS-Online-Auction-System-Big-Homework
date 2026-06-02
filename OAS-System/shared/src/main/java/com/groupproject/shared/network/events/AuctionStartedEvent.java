package com.groupproject.shared.network.events;

public class AuctionStartedEvent extends ServerEvent {
    private int auctionId;

    public AuctionStartedEvent(int auctionId) {
        this.auctionId = auctionId;
    }
}
