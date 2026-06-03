package com.groupproject.shared.network.responses;
import com.groupproject.shared.model.transaction.AuctionDetail;

public class GetAuctionDetailResponse extends Response  {
    private AuctionDetail auctionDetail;
    public GetAuctionDetailResponse(boolean success, AuctionDetail auctionDetail, String message) {
        super(success,message);
        this.auctionDetail=auctionDetail;
    }
    public GetAuctionDetailResponse(boolean success, String message) {
        super(success,message);
    }
    public AuctionDetail getAuctionDetail() {
        return auctionDetail;
    }
}