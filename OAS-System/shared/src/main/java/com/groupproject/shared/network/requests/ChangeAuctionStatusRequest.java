package com.groupproject.shared.network.requests;

import com.groupproject.shared.model.enums.AuctionStatus;

public class ChangeAuctionStatusRequest extends Request {
    private int auctionId;
    private AuctionStatus newStatus;

    public ChangeAuctionStatusRequest(int auctionId, AuctionStatus newStatus) {
        this.auctionId = auctionId;
        this.newStatus = newStatus;
    }

    public int getAuctionId() { return auctionId; }
    public AuctionStatus getNewStatus() { return newStatus; }
}
