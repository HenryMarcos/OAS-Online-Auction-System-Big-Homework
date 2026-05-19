package com.groupproject.shared.network.AuctionEvent;
import java.io.Serializable;

public abstract class AuctionEvent implements Serializable{
    private final int auctionId;
    public AuctionEvent(int auctionId) {
        this.auctionId= auctionId;
    }
    public int getAuctionId() {
        return auctionId;
    }
    public abstract void accept(AuctionListener listener) ;
}