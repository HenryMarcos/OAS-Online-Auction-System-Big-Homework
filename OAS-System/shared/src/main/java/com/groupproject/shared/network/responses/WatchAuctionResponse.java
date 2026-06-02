package com.groupproject.shared.network.responses;

public class WatchAuctionResponse extends Response {
    private static final long serialVersionUID = 1L;
    private int auctionId;

    public WatchAuctionResponse(boolean success, String message, int auctionId) {
        super(success, message);
        this.auctionId = auctionId;
    }

    public int getAuctionId() {
        return auctionId;
    }
}
