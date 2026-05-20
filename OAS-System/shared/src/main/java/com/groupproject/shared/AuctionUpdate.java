package com.groupproject.shared;

import java.io.Serializable;

public class AuctionUpdate implements Serializable {
    private int auctionId;
    private double newBidAmount;
    private int newHighestBidderId;

    public AuctionUpdate(int auctionId, int newHighestBidderId, double newBidAmount) {
        this.auctionId = auctionId;
        this.newHighestBidderId = newHighestBidderId;
        this.newBidAmount = newBidAmount;
    }

    /* 
    public AuctionUpdate(BidRequest bidRequest) {
        this(bidRequest.getAuctionId(), bidRequest.getBidderUsername(), bidRequest.getBidAmount());
    }
    */

    public String getType() { return "AUCTION_UPDATE"; }
}
