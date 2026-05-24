package com.groupproject.shared.network.event;

public class AuctionEndedEvent extends ServerEvent {
    private int auctionId;
    private int winnerId;
    private double finalPrice;

    public AuctionEndedEvent(int auctionId, int winnerId, double finalPrice) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.finalPrice = finalPrice;
    }

    public int getAuctionId() { 
        return auctionId; 
    }
    
    public int getWinnerId() { 
        return winnerId; 
    }
    
    public double getFinalPrice() { 
        return finalPrice; 
    }
}