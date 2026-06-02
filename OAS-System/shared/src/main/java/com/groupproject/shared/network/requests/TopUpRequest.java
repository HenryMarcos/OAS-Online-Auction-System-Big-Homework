package com.groupproject.shared.network.requests;

public class TopUpRequest extends Request {
    private static final long serialVersionUID = 1L;

    private double amount;

    public TopUpRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() { return amount; }
}
