package com.groupproject.shared;

import java.io.Serializable;

public class BidRequest implements Serializable {
    private int auctionId;
    private String bidderUsername;
    private double bidAmount;

    public BidRequest(int auctionId, String bidderUsername, double bidAmount) {
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
    }

    public int getAuctionId() { return auctionId; }
    public String getBidderUsername() { return bidderUsername; }
    public double getBidAmount() { return bidAmount; }

    public String getType() { return "BID"; }
}
