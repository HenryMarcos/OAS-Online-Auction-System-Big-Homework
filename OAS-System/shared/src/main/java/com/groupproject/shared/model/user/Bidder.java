package com.groupproject.shared.model.user;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private double accountBalance;
    public Bidder() {
        super();
    }
    public Bidder(String username, String password, String email, double accountBalance) {
        super(username, password, email);
        this.accountBalance=100000.0;
    }
    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }

    public double getAccountBalance() {
        return accountBalance;
    }
}
