package com.groupproject.shared.network.responses;

public class TopUpResponse extends Response {
    private static final long serialVersionUID = 1L;

    private double newBalance;

    public TopUpResponse(boolean success, String message, double newBalance) {
        super(success, message);
        this.newBalance = newBalance;
    }

    public TopUpResponse(boolean success, String message) {
        this(success, message, 0.0);
    }

    public double getNewBalance() { return newBalance; }
}
