package com.groupproject.shared.network;

import java.util.List;

import com.groupproject.shared.model.transaction.Auction;

public class GetMyAuctionsResponse extends Response {
    private static final long serialVersionUID = 1L;
    
    // Chỉ cần khai báo thêm những dữ liệu đặc thù của Response này
    private List<Auction> myAuctions;

    public GetMyAuctionsResponse(boolean success, String message, List<Auction> myAuctions) {
        // Bắt buộc phải gọi super() lên lớp cha đầu tiên
        super(success, message); 
        this.myAuctions = myAuctions;
    }

    // Lớp cha đã lo phần getSuccess() và getMessage(), ta chỉ cần viết get/set cho phần mở rộng
    public List<Auction> getMyAuctions() { 
        return myAuctions; 
    }

    public void setMyAuctions(List<Auction> myAuctions) { 
        this.myAuctions = myAuctions; 
    }
}