package com.groupproject.shared.network.responses;

import java.util.List;

import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.transaction.BidDTO;

public class JoinAuctionResponse extends Response {
    private Auction auction;
    private List<BidDTO> pastBids;

    public JoinAuctionResponse(boolean success, Auction auction, List<BidDTO> pastBids, String message) {
        super(success, message);
        this.auction = auction;
        this.pastBids = pastBids;
    }

    public JoinAuctionResponse(boolean success, String message) {
        super(success, message);
    }

    public Auction getAuction() { return auction; }
    public List<BidDTO> getPastBids() { return pastBids; }
}