package com.groupproject.shared.network;
import java.util.List;
import com.groupproject.shared.model.transaction.Auction;
public class GetAuctionResponse extends Response {
    private List<Auction> selectedAuctions;
    public GetAuctionResponse(boolean success,String message, List<Auction> auctions) {
        super(success, message);
        this.selectedAuctions= auctions;
    }
    public List<Auction> getAuction() {
        return selectedAuctions;
    }
    // HANDLE LATER
    @Override
    public String getType() {
        return "GET_AUCTIONS_RESULT";
    }
}
