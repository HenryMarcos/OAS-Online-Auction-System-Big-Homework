package com.groupproject.shared;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;                         // Sản phẩm mang ra đấu giá
    private Seller seller;                     // Người đăng bán
    private List<BidTransaction> bidHistory;   // Danh sách các lượt đặt giá
    private boolean isActive;                  // Phiên đấu giá còn mở không?

    public Auction(Item item, Seller seller) {
        super(); // Tạo ID cho phiên đấu giá
        this.item = item;
        this.seller = seller;
        this.bidHistory = new ArrayList<>();   // Khởi tạo danh sách trống
        this.isActive = true;                  // Mặc định tạo ra là mở cửa
    }

    // Hàm dùng để thêm một lượt đặt giá mới vào lịch sử
    public void addBid(BidTransaction transaction) {
        if (isActive) {
            bidHistory.add(transaction);
        } else {
            System.out.println("Phiên đấu giá đã kết thúc, không thể đặt giá thêm!");
        }
    }

    // --- Getters ---
    public Item getItem() { return item; }
    public Seller getSeller() { return seller; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public boolean isActive() { return isActive; }
    
    // Đóng phiên đấu giá
    public void endAuction() { this.isActive = false; }
}
