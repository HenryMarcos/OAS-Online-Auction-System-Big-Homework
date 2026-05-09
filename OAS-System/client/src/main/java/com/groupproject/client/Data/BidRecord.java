package com.groupproject.client.Data;
// tạo một class để lưu những điểm cần vẽ lên trên Table trong mỗi phiên đấu giá 

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class BidRecord {
    private SimpleIntegerProperty bidderId;
    private SimpleStringProperty bidder;
    private SimpleDoubleProperty bidderAmount;
    private SimpleStringProperty currentTime;
    public BidRecord(int bidderId, String bidder, double bidderAmount , String currentTime) {
        this.bidderId=new SimpleIntegerProperty(bidderId);
        this.bidder= new SimpleStringProperty(bidder);
        this.bidderAmount= new SimpleDoubleProperty(bidderAmount);
        this.currentTime= new SimpleStringProperty(currentTime);
    }
    public int getBidderid() {
        return bidderId.get();
    }
    public String getBidder() {
        return bidder.get();
    }
    public double getBidderAmount() {
        return bidderAmount.get();
    }
    public String getCurrentTime() {
        return currentTime.get();
    }
}
