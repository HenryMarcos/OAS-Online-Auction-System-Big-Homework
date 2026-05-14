package com.groupproject.shared.model.user;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    private double accountBalance; // Số dư tài khoản của người đấu giá
    private String shippingAddress; // Địa chỉ giao hàng của người đấu giá

    public Bidder(int id, String username, String password, String email, double accountBalance, String shippingAddress) {
        super(id, username, password, email);
        this.accountBalance = accountBalance;
        this.shippingAddress = shippingAddress;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp vào phải lớn hơn 0.");
        }
        this.accountBalance += amount; // Nạp tiền vào tài khoản
    }

    public void deduct(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền rút ra phải lớn hơn 0.");
        }
        if (amount > this.accountBalance) {
            throw new IllegalArgumentException("Số dư tài khoản không đủ để thực hiện giao dịch.");
        }
        this.accountBalance -= amount; // Rút tiền từ tài khoản
    }

    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }
}
