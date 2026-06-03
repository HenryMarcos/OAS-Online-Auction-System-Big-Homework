package com.groupproject.shared.network.requests;

public class WatchAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;
    private int auctionId;

    public WatchAuctionRequest(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() { return auctionId; }
}
