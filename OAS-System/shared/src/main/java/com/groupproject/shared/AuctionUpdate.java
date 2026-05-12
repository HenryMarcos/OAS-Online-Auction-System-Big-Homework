package com.groupproject.shared;

import java.io.Serializable;

public class AuctionUpdate implements Serializable {
    private int auctionId;
    private double newBidAmount;
    private String newHighestBidder;
    // thêm thời gian người này đặt
    public AuctionUpdate(int auctionId, String newHighestBidder, double newBidAmount)  {
        this.auctionId = auctionId;
        this.newHighestBidder = newHighestBidder;
        this.newBidAmount = newBidAmount;
    }
    public AuctionUpdate(BidRequest bidRequest) {
        this.auctionId= bidRequest.getAuctionId();
        this.newBidAmount= bidRequest.getBidAmount();
        this.newHighestBidder = bidRequest.getBidderUsername();
    }
    public String getType() { return "AUCTION_UPDATE"; }
}
