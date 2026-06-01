package com.groupproject.shared.network;

public class AuctionWonNotification implements Wallet {
    private final int auctionId;
    private final int sellerId;
    private final double finalPrice;
    private final int highestBidderId;
    private final double total_balance;
    private final double locked_balance;
    private final long timestamp;

    public AuctionWonNotification(int auctionId, int sellerId, double finalPrice,int highestBidderId, double total_balance, double locked_balance,long timestamp) {
        this.auctionId=auctionId;
        this.sellerId=sellerId;
        this.finalPrice=finalPrice;
        this.highestBidderId=highestBidderId;
        this.total_balance=total_balance;
        this.locked_balance=locked_balance;
        this.timestamp=timestamp;
    }
    @Override
    public boolean hasWalletUpdated() {
        return true;
    }
    public int getAuctionId() {
        return auctionId;
    }
    public int getSellerId() {
        return sellerId;
    }
    public int getHighestBidderId() {
        return highestBidderId;
    }
    public long getTimeStamp() {
        return timestamp;
    }
    public double getFinalPrice() {
        return finalPrice;
    }
    @Override
    public double getAvailableBalance() {
        return total_balance-locked_balance;
    }

}
