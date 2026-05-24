package com.groupproject.shared.network.event;

public class AuctionCancelledEvent extends ServerEvent {
    private int auctionId;
    private String reason;

    public AuctionCancelledEvent(int auctionId, String reason) {
        this.auctionId = auctionId;
        this.reason = reason;
    }

    public int getAuctionId() { return auctionId; }
    public String getReason() { return reason; }
}