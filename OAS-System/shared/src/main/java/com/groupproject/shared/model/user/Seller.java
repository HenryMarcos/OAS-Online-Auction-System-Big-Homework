package com.groupproject.shared.model.user;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    private int ratingNumber; // Số lượng đánh giá mà người bán đã nhận được, có thể được sử dụng để tính toán đánh giá trung bình của người bán
    private float rating; // Đánh giá trung bình của người bán, có thể được tính dựa trên phản hồi của người mua sau mỗi giao dịch
    private String bankAccount; // Thông tin tài khoản ngân hàng của người bán, dùng để nhận tiền sau khi bán được sản phẩm
    private boolean isVerified; // Trạng thái xác minh của người bán, có thể được sử dụng để tăng độ tin cậy và uy tín của người bán trên nền

    public Seller() {
        super();
        this.ratingNumber = 0; 
        this.rating = 0.0f; 
        this.bankAccount = ""; 
        this.isVerified = false; 
    }

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.ratingNumber = 0; 
        this.rating = 0.0f; 
        this.bankAccount = ""; 
        this.isVerified = false; 
    }

    public Seller(int ratingNumber, float rating, String bankAccount, boolean isVerified, String username, String password, String email) {
        super(username, password, email);
        this.ratingNumber = ratingNumber;
        this.rating = rating;
        this.bankAccount = bankAccount;
        this.isVerified = isVerified;
    }

    public void setRating(float rating) {
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Đánh giá phải nằm trong khoảng từ 0 đến 5.");
        }
        this.rating = rating;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public void setVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public void updateRating(float newRating) {
        if (newRating < 0 || newRating > 5) {
            throw new IllegalArgumentException("Đánh giá phải nằm trong khoảng từ 0 đến 5.");
        }
        // Cập nhật đánh giá trung bình của người bán, có thể được gọi sau mỗi giao dịch để điều chỉnh đánh giá dựa trên phản hồi của người mua
        this.rating = (this.rating * ratingNumber + newRating) / (ratingNumber + 1);
        this.ratingNumber++; 
    }

    public int getRatingNumber() {
        return ratingNumber;
    }

    public float getRating() {
        return rating;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public boolean isVerified() {
        return isVerified;
    }
}
