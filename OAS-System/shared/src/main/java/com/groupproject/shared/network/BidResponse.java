package com.groupproject.shared.network;

public class BidResponse extends Response {
    private double currentBid;
    private Integer auctionId;
    public BidResponse(boolean success, String message,int auctionId,double currentBid) {
        super(success,message);
        this.auctionId=auctionId;
        this.currentBid=currentBid;
    }
    public int getAuctionId() {
        return auctionId;
    }
    public double getCurrentBid() {
        return currentBid;
    }
    // ĐỂ XỬ LÝ SAU 

    @Override
    public String getType() {
        return "";
    }
}
