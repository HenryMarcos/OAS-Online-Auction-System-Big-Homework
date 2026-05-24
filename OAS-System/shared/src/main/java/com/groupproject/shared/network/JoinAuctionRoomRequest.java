package com.groupproject.shared.network;

public class JoinAuctionRoomRequest extends Request {
    private final int auctionId;

    public JoinAuctionRoomRequest(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionId() {
        return auctionId;
    }
}