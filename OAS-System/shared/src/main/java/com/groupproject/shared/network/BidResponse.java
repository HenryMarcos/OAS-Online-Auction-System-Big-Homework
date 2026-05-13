package com.groupproject.shared.network;

public class BidResponse extends Response {
    private double currentBid;
    private int auctionId;
    public BidResponse(boolean success, String message) {
        super(success,message);
    }
    
    @Override
    public String getType() {
        return "";
    }
}
