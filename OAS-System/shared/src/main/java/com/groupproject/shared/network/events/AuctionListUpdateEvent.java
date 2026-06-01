package com.groupproject.shared.network.events;

import java.time.LocalDateTime;
import java.util.List;

import com.groupproject.shared.model.transaction.Auction;

public class AuctionListUpdateEvent extends ServerEvent{
    private List<Auction> activeAuctions;
    private final LocalDateTime serverTime;

    public AuctionListUpdateEvent(List<Auction> activeAuctions, LocalDateTime serverTime) {
        this.activeAuctions = activeAuctions;
        this.serverTime = serverTime;
    }

    public List<Auction> getActiveAuctions() { return activeAuctions; }
    public LocalDateTime getServerTime() { return serverTime; }
}
