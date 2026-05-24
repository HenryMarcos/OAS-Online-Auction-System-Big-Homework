package com.groupproject.shared.network.response;

import com.groupproject.shared.model.transaction.Auction;

public class ChangeAuctionStatusResponse extends Response {
    private Auction updatedAuction; // Chứa thông tin phiên đấu giá sau khi đã cập nhật

    /**
     * Constructor dùng khi đổi trạng thái THÀNH CÔNG
     * @param success true
     * @param message Lời nhắn (VD: "Auction started successfully")
     * @param updatedAuction Phiên đấu giá với trạng thái mới nhất từ DB
     */
    public ChangeAuctionStatusResponse(boolean success, String message, Auction updatedAuction) {
        super(success, message);
        this.updatedAuction = updatedAuction;
    }

    /**
     * Constructor dùng khi đổi trạng thái THẤT BẠI
     * @param success false
     * @param message Lời nhắn lỗi
     */
    public ChangeAuctionStatusResponse(boolean success, String message) {
        super(success, message);
        this.updatedAuction = null;
    }

    public Auction getUpdatedAuction() {
        return updatedAuction;
    }
}