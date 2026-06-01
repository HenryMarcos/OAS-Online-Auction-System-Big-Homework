package com.groupproject.shared.network.events;

public class AuctionResultEvent extends ServerEvent {
    private int auctionId;
    private String auctionTitle;
    private boolean isWinner;
    private double winningPrice;

    public AuctionResultEvent(int auctionId, String auctionTitle, boolean isWinner, double winningPrice) {
        this.auctionId = auctionId;
        this.auctionTitle = auctionTitle;
        this.isWinner = isWinner;
        this.winningPrice = winningPrice;
    }

    public int getAuctionId() { return auctionId; }
    public String getAuctionTitle() { return auctionTitle; }
    public boolean isWinner() { return isWinner; }
    public double getWinningPrice() { return winningPrice; }
}
