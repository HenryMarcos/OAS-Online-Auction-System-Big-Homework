package com.groupproject.shared.network.responses;

public class UnwatchAuctionResponse extends Response {
    private static final long serialVersionUID = 1L;
    private int auctionId;

    public UnwatchAuctionResponse(boolean success, String message, int auctionId) {
        super(success, message);
        this.auctionId = auctionId;
    }

    public int getAuctionId() {
        return auctionId;
    }
}
