package com.groupproject.shared.network.request;

public class TopUpRequest extends Request {
    private double amount;

    public TopUpRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }
}