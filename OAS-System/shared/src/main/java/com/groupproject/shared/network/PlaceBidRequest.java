package com.groupproject.shared.network;

public class PlaceBidRequest extends Request {
    private int auctionId;
    private int bidderId;
    private double bidAmount;

    public PlaceBidRequest(int auctionId, int bidderId, int bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
    }

    public int getAuctionId() { return auctionId; }
    public int getBidderId() { return bidderId; }
    public double getBidAmount() { return bidAmount; }
}
