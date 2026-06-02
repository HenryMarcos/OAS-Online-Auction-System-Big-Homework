package com.groupproject.shared.network.requests;

public class UnwatchAuctionRequest extends Request {
    private static final long serialVersionUID = 1L;
    private int auctionId;

    public UnwatchAuctionRequest(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() { return auctionId; }
}
