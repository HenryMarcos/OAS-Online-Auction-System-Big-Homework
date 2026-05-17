package com.groupproject.shared.network.AuctionEvent;
import java.io.Serializable;

import com.groupproject.shared.network.NetworkMessage;

public abstract class AuctionEvent implements Serializable,NetworkMessage {
    private final int auctionId;
    public AuctionEvent(int auctionId) {
        this.auctionId= auctionId;
    }
    public int getAuctionId() {
        return auctionId;
    }
    public abstract void accept(AuctionListener listener) ;
}