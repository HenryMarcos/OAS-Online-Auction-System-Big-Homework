package com.groupproject.client.Utlis;

public class BidHistory {
    private String bidder;
    private double price;
    private String time;
    public BidHistory(String bidder, double price, String time) {
        this.bidder= bidder;
        this.price= price;
        this.time=time;
    }
    public void setBidder(String bidder) {
        this.bidder= bidder;
    }
    public void setPrice(double price) {
        this.price= price;
    }
    public void setTime(String time) {
        this.time=time;
    }
    public String getBidder() {
        return bidder;
    }
    public String getTime() {
        return time;
    }
    public double getPrice() {
        return price;
    }
}
