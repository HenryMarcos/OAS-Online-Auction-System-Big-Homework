package com.groupproject.shared.network.events;

import java.util.List;

import com.groupproject.shared.model.transaction.Auction;

public class AuctionListUpdateEvent extends ServerEvent{
    private List<Auction> activeAuctions;

    public AuctionListUpdateEvent(List<Auction> activeAuctions) {
        this.activeAuctions = activeAuctions;
    }

    public List<Auction> getActiveAuctions() { 
        return activeAuctions; 
    }
}
