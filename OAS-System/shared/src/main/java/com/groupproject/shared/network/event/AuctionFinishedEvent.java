package com.groupproject.shared.network.event;

public class AuctionFinishedEvent extends ServerEvent {
    private int auctionId;

    public AuctionFinishedEvent(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() { 
        return auctionId; 
    }
}