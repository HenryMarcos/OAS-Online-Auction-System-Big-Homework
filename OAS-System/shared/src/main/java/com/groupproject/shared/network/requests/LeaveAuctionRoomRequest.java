package com.groupproject.shared.network.requests;

public class LeaveAuctionRoomRequest extends Request {
    private int auctionId;

    public LeaveAuctionRoomRequest(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() { return auctionId; }
}
