package com.groupproject.shared.model.transaction;


import com.groupproject.shared.model.base.Entity;


public class Auction extends Entity {
    private static final long serialVersionUID = 1L;
    // có thể có thêm phần lưu lịch sử của những người tham gia đấu giá 
    private AuctionItem auctionItem; // mối quan hệ has-a
    public Auction(AuctionItem auctionItem) {
        this.auctionItem= auctionItem;
    }
    public void setAuctionItem(AuctionItem auctionItem) {
        this.auctionItem=auctionItem;
    }
    public AuctionItem getAuctionItem() {
        return auctionItem;
    }
    
}
