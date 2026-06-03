package com.groupproject.shared.network.responses;

import com.groupproject.shared.model.transaction.Auction;

public class ChangeAuctionStatusResponse extends Response {
    private Auction updatedAuction;

    public ChangeAuctionStatusResponse(boolean isSuccess, String message) {
        super(isSuccess, message);
    }
    
    public ChangeAuctionStatusResponse(boolean isSuccess, String message, Auction updatedAuction) {
        super(isSuccess, message);
        this.updatedAuction = updatedAuction;
    }

    public Auction getUpdatedAuction() { return updatedAuction; }
}
