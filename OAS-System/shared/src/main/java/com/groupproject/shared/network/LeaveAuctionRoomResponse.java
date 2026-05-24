package com.groupproject.shared.network;

public class LeaveAuctionRoomResponse extends Response {

    public LeaveAuctionRoomResponse(boolean success, String message) {
        super(success, message);
    }
    
    public LeaveAuctionRoomResponse(boolean success) {
        super(success);
    }
}