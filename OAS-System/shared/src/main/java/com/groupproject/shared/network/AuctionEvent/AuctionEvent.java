package com.groupproject.shared.network.AuctionEvent;

import java.io.Serializable;

public abstract class AuctionEvent implements Serializable {
    private final int auctionId;
    protected AuctionEvent(int auctionId) {
        this.auctionId= auctionId;
    }
    public int getAuctionId() {
        return auctionId;
    }
}