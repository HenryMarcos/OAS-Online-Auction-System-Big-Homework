package com.groupproject.shared.network;
import com.groupproject.shared.model.transaction.Auction;

public class GetAuctionDetailResponse extends Response  {
    private Auction auction;
    public GetAuctionDetailResponse(boolean success,String message,Auction auction) {
        super(success,message);
        this.auction=auction;
    }
    public Auction getAuction() {
        return auction;
    }
    // Về sau thì thêm vào ;
    @Override
    public String getType() {
        return "GET_AUCTION_RESULT";
    }
}