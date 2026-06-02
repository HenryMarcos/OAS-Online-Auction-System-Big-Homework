package com.groupproject.shared.network.events;

public class BalanceUpdateEvent extends ServerEvent {
    private final int userId;
    private final double newBalance;

    public BalanceUpdateEvent(int userId, double newBalance) {
        this.userId = userId;
        this.newBalance = newBalance;
    }

    public int getUserId() { return userId; }
    public double getNewBalance() { return newBalance; }
}
