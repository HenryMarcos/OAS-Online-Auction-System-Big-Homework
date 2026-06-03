package com.groupproject.shared.network.requests;

public class PlaceBidRequest extends Request {
    private int auctionId;
    private double bidAmount;

    // Đã sửa lại lỗi int bidAmount -> double bidAmount
    public PlaceBidRequest(int auctionId, double bidAmount) { 
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
    }

    public int getAuctionId() { return auctionId; }
    public double getBidAmount() { return bidAmount; }
}
