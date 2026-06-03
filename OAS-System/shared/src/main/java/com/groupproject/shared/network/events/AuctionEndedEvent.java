package com.groupproject.shared.network.events;

public class AuctionEndedEvent extends ServerEvent {
    private final int auctionId;
    private final Integer winnerId;
    private final double winningBidAmount;

    public AuctionEndedEvent(int auctionId, Integer winnerId, double winningBidAmount) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.winningBidAmount = winningBidAmount;
    }

    public int getAuctionId() { return auctionId; }
    public Integer getWinnerId() { return winnerId; }
    public double getWinningBidAmount() { return winningBidAmount; }
}
