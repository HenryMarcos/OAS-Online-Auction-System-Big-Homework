package com.groupproject.shared.network.request;

import com.groupproject.shared.model.enums.AuctionStatus;

public class ChangeAuctionStatusRequest extends Request {
    private int auctionId;
    private AuctionStatus newStatus;

    /**
     * Constructor tạo request đổi trạng thái 
     * (WAITING -> ACTIVE; WAITING/ SCHEDULED/ ACTIVE -> CANCELLED)
     * @param auctionId ID của phiên đấu giá cần đổi
     * @param newStatus Trạng thái mới (Ví dụ: AuctionStatus.ACTIVED)
     */
    /*TODO: Chuyển trạng thái WAITING -> ACTIVE bằng nút "Bắt đầu đấu giá"
    Chuyển trạng thái WAITING/ SCHEDULED/ ACTIVE -> CANCELLED bằng nút "Hủy đấu giá"
    */
    public ChangeAuctionStatusRequest(int auctionId, AuctionStatus newStatus) {
        this.auctionId = auctionId;
        this.newStatus = newStatus;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public AuctionStatus getNewStatus() {
        return newStatus;
    }
}