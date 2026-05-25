package com.groupproject.shared.network.response;

public class TopUpResponse extends Response {
    private double newBalance;

    public TopUpResponse(boolean success, String message, double newBalance) {
        super(success, message);
        this.newBalance = newBalance;
    }

    public double getNewBalance() {
        return newBalance;
    }
}