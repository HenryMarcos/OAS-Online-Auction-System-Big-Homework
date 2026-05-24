package com.groupproject.shared.network;
import com.groupproject.shared.model.transaction.AuctionDetail;
import com.groupproject.shared.model.transaction.BidTransaction;
import java.util.List;
public class GetAuctionDetailResponse extends Response  {
    private AuctionDetail auctionDetail;
    private List<BidTransaction> bidHistory;
    public GetAuctionDetailResponse(boolean success,String message,AuctionDetail auctionDetail, List<BidTransaction> bidHistory) {
        super(success,message);
        this.auctionDetail=auctionDetail;
    }
    public AuctionDetail getAuctionDetail() {
        return auctionDetail;
    }
    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }
    // Về sau thì thêm vào ;
    @Override
    public String getType() {
        return "GET_AUCTION_DETAIL_RESULT";
    }
}