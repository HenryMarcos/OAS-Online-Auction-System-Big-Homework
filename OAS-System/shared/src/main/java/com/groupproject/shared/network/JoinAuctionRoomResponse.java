package com.groupproject.shared.network;

public class JoinAuctionRoomResponse extends Response {
    public JoinAuctionRoomResponse(boolean success, String message) {
        // Chỉ cần gọi super để nạp dữ liệu vào lớp cha là đủ
        super(success, message); 
    }
}