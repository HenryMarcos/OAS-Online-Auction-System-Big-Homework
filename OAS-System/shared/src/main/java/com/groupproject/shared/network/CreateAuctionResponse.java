package com.groupproject.shared.network;

import com.groupproject.shared.model.transaction.Auction;

public class CreateAuctionResponse extends Response {
    private Auction newlyCreatedAuction;

    public CreateAuctionResponse(boolean success, Auction newlyCreatedAuction, String message) {
        super(success, message);
        this.newlyCreatedAuction = newlyCreatedAuction;
    }
    
    public Auction getAuction() { return newlyCreatedAuction; }

}
