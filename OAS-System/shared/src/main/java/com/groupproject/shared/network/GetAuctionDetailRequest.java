package com.groupproject.shared.network;

public class GetAuctionDetailRequest extends Request  {
    private int auctionId;
    public GetAuctionDetailRequest(int auction_id) {
        this.auctionId = auction_id;
    }
    public int getAuctionId() {
        return auctionId;
    }
}