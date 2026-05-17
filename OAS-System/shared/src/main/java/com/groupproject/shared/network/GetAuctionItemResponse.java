package com.groupproject.shared.network;
import java.util.List;
import com.groupproject.shared.model.transaction.AuctionItem;
public class GetAuctionItemResponse extends Response {
    private List<AuctionItem> selectedAuctions;
    public GetAuctionItemResponse(boolean success,String message, List<AuctionItem> auctions) {
        super(success, message);
        this.selectedAuctions= auctions;
    }
    public List<AuctionItem> getAuction() {
        return selectedAuctions;
    }
    // HANDLE LATER
    @Override
    public String getType() {
        return "GET_AUCTIONS_Item_RESULT";
    }
}
