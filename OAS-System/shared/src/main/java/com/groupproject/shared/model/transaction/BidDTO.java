package com.groupproject.shared.model.transaction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String bidderName; // Can be User ID or actual name
    private double amount;
    private LocalDateTime bidTime;

    public BidDTO(String bidderName, double amount, LocalDateTime bidTime) {
        this.bidderName = bidderName;
        this.amount = amount;
        this.bidTime = bidTime;
    }

    public String getBidderName() { return bidderName; }
    public double getAmount() { return amount; }
    public LocalDateTime getBidTime() { return bidTime; }
    
    // Helper method for the TableView to display formatted time cleanly
    public String getTimeString() {
        if (bidTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return bidTime.format(formatter);
    }
}