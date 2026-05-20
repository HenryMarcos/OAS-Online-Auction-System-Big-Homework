package com.groupproject.shared.network;

public class BidRequest extends Request {
    private int auctionId;
    private int bidderId;
    private double bidAmount;

    public BidRequest(int auctionId, int bidderId, double bidAmount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
    }

    public int getAuctionId() { return auctionId; }
    public int getBidderId() { return bidderId; }
    public double getBidAmount() { return bidAmount; }

    public String getType() { return "BID"; }
}
