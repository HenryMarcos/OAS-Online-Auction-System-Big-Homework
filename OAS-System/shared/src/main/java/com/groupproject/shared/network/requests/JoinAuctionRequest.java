package com.groupproject.shared.network.requests;

public class JoinAuctionRequest extends Request {
    private int auctionId;

    public JoinAuctionRequest(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() { return auctionId; }
}
