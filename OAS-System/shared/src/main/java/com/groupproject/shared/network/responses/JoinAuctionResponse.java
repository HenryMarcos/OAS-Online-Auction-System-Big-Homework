package com.groupproject.shared.network.responses;

import com.groupproject.shared.model.transaction.AuctionDetail;

public class JoinAuctionResponse extends Response {
    private AuctionDetail auctionDetail;

    public JoinAuctionResponse(boolean success, AuctionDetail auctionDetail, String message) {
        super(success, message);
        this.auctionDetail = auctionDetail;
    }

    public JoinAuctionResponse(boolean success, String message) {
        super(success, message);
    }

    public AuctionDetail getAuctionDetail() { return auctionDetail; }
}