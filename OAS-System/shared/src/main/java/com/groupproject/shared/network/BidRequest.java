package com.groupproject.shared.network;

public class BidRequest extends Request {
    private int auctionId;
    private String bidderUsername;
    private double bidAmount;

    public BidRequest(int auctionId, String bidderUsername, double bidAmount) {
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
    }

    public Integer getAuctionId() { return auctionId; }
    public String getBidderUsername() { return bidderUsername; }
    public double getBidAmount() { return bidAmount; }

}
