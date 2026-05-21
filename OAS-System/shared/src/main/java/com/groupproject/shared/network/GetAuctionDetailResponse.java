package com.groupproject.shared.network;
import com.groupproject.shared.model.transaction.AuctionDetail;

public class GetAuctionDetailResponse extends Response  {
    private AuctionDetail auctionDetail;
    public GetAuctionDetailResponse(boolean success,String message,AuctionDetail auctionDetail) {
        super(success,message);
        this.auctionDetail=auctionDetail;
    }
    public AuctionDetail getAuctionDetail() {
        return auctionDetail;
    }
    // Về sau thì thêm vào ;
    @Override
    public String getType() {
        return "GET_AUCTION_DETAIL_RESULT";
    }
}