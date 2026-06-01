package com.groupproject.shared.network.AuctionEvent;
import java.io.Serializable;
import com.groupproject.shared.network.events.ServerEvent;

public abstract class AuctionEvent extends ServerEvent {
    private final int auctionId;
    public AuctionEvent(int auctionId) {
        this.auctionId= auctionId;
    }
    public int getAuctionId() {
        return auctionId;
    }
    public abstract void accept(AuctionListener listener) ;
}