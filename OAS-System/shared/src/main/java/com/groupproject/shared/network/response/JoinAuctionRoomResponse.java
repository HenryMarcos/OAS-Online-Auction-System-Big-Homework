package com.groupproject.shared.network.response;

import com.groupproject.shared.model.transaction.Auction;

public class JoinAuctionRoomResponse extends Response {
    private Auction auctionDetails;

    public JoinAuctionRoomResponse(boolean success, String message, Auction auctionDetails) {
        // Chỉ cần gọi super để nạp dữ liệu vào lớp cha là đủ
        super(success, message);
        this.auctionDetails = auctionDetails;
    }

    public JoinAuctionRoomResponse(boolean success, String message) {
        super(success, message);
    }

    public void setAuctionDetails(Auction auctionDetails) {
        this.auctionDetails = auctionDetails;
    }

    public Auction getAuctionDetails() {
        return auctionDetails;
    }
}