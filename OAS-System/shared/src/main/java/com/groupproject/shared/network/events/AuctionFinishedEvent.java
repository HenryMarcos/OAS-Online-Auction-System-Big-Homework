package com.groupproject.shared.network.events;

public class AuctionFinishedEvent extends ServerEvent {
    private int auctionId;

    public AuctionFinishedEvent(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() { 
        return auctionId; 
    }
}