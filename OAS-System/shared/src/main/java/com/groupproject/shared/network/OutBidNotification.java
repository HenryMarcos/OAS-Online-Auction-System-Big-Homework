package com.groupproject.shared.network;

import java.io.Serializable;
// KHI CÓ AI ĐÓ TRONG PHÒNG ĐẤU GIÁ ĐẶT VƯỢT GIÁ BẠN ĐÃ ĐẶT
public class OutBidNotification implements Serializable,Wallet {
    private final int auctionId;
    private final double newBidAmount;
    private final double total_balance;
    private final double locked_balance;
    private final long timestamp;
    public OutBidNotification(int auctionId,double newBidAmount, double total_balance, double locked_balance,long timestamp) {
        this.auctionId=auctionId;
        this.newBidAmount= newBidAmount;
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
    public double getNewBidAmount() {
        return newBidAmount;
    }
    public long getTimeStamp() {
        return timestamp;
    }
    @Override
    public double getAvailableBalance() {
        return total_balance- locked_balance;
    }
}
