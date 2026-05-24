package com.groupproject.shared.network;

public class GetMyAuctionsRequest extends Request {
    private int sellerId;

    // Constructor tạo request lấy TẤT CẢ danh sách các phiên đấu giá của người bán (có cả ENDED, CANCELLED, FINISHED)
    /*TODO: Gọi request này khi người dùng muốn xem các phiên đấu giá của mình 
    để bấm các nút "Bắt đầu đấu giá" và "Hủy đấu giá" 
    */
    public GetMyAuctionsRequest(int sellerId) {
        this.sellerId = sellerId;
    }

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }
}