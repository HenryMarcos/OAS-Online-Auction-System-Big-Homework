package com.groupproject.shared.network;

public class JoinAuctionRequest extends Request{
    private int auctionId;
    private int userId;
    public JoinAuctionRequest(int auctionId, int userId) {
        this.auctionId= auctionId;
        this.userId= userId;
    }
    public int getAuctionId() {
        return auctionId;
    }
    public int getUserId() {
        return userId;
    }
    
}
