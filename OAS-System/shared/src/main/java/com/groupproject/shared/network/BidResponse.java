package com.groupproject.shared.network;

public class BidResponse extends Response implements Wallet {
    private final  double currentBid;
    private final int auctionId;
    private final double total_balance;
    private final double locked_balance;
    public BidResponse(boolean success, String message,int auctionId,double currentBid, double total_balance,double locked_balance) {
        super(success,message);
        this.auctionId=auctionId;
        this.currentBid=currentBid;
        this.total_balance=total_balance;
        this.locked_balance= locked_balance;
    }
    @Override
    public boolean hasWalletUpdated() {
        return isSuccess(); 
    }
    public int getAuctionId() {
        return auctionId;
    }
    public double getCurrentBid() {
        return currentBid;
    }
    @Override
    public double getAvailableBalance() {
        return total_balance-locked_balance;
    }
    // ĐỂ XỬ LÝ SAU 

    @Override
    public String getType() {
        return "";
    }
}
