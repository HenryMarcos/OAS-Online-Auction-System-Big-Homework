package com.groupproject.shared.network;
import com.groupproject.shared.model.transaction.AuctionItem;
import com.groupproject.shared.model.transaction.Auction;


public class CreateAuctionResponse extends Response {
    private Auction newlyCreatedAuction;
    private AuctionItem newAuctionItem;
    public CreateAuctionResponse(boolean success, Auction newlyCreatedAuction, String message,AuctionItem newAuctionItem) {
        super(success, message);
        this.newlyCreatedAuction = newlyCreatedAuction;
        this.newAuctionItem= newAuctionItem;
    }
    
    public Auction getAuction() { return newlyCreatedAuction; }
    public AuctionItem getAuctionItem() {return newAuctionItem;}
    @Override
    public String getType() {
        return "CREATE_AUCTION";
    }
}