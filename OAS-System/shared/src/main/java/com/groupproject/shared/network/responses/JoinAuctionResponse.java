package com.groupproject.shared.network.responses;

import com.groupproject.shared.model.transaction.Auction;

public class JoinAuctionResponse extends Response {
    private Auction auction;

    public JoinAuctionResponse(boolean success, Auction auction, String message) {
        super(success, message);
        this.auction = auction;
    }

    public JoinAuctionResponse(boolean success, String message) {
        super(success, message);
    }

    public Auction getAuction() { return auction; }
}