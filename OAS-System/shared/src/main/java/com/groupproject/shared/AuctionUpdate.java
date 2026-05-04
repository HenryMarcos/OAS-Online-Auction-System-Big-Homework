package com.groupproject.shared;

import java.io.Serializable;

public class AuctionUpdate implements Serializable {
    private int auctionId;
    private double newBidAmount;
    private String newHighestBidder;

    public AuctionUpdate(int auctionId, String newHighestBidder, double newBidAmount) {
        this.auctionId = auctionId;
        this.newHighestBidder = newHighestBidder;
        this.newBidAmount = newBidAmount;
    }

    public AuctionUpdate(BidRequest bidRequest) {
        this(bidRequest.getAuctionId(), bidRequest.getBidderUsername(), bidRequest.getBidAmount());
    }

    public String getType() { return "AUCTION_UPDATE"; }
}
