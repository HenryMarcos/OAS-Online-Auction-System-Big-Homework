package com.groupproject.shared.network.request;

public class LeaveAuctionRoomRequest extends Request {
    private final int auctionId;

    public LeaveAuctionRoomRequest(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() {
        return auctionId;
    }
}