package com.groupproject.shared.network;
// HÀM NÀY SẼ ĐƯỢC SỬ DỤNG ĐÊ LẤY RA LỊCH SỬ CÁC CUỘC GIAO DỊCH TRONG MỘT AUCTION
public class BidRequest extends Request {
    private int auctionId;
    private double newBidAmount;
    private String newHighestBidder;
    public BidRequest(int auctionId,String newHighestBidder, double newBidAmount) {
        this.auctionId= auctionId;
        this.newHighestBidder=newHighestBidder;
        this.newBidAmount= newBidAmount;
    }
    

    public int getAuctionId() { return auctionId; }
    public String getBidderUsername() {
        return newHighestBidder;
    }
    public double getBidAmount() {
        return newBidAmount;
    }
}
