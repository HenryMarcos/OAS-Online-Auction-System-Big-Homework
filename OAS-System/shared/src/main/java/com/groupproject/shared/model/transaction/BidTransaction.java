package com.groupproject.shared.model.transaction;

import java.time.format.DateTimeFormatter;

import com.groupproject.shared.model.base.Entity;

public class BidTransaction extends Entity {
    private static final long serialVersionUID = 1L;

    private int bidderId; // ID của người đặt giá thầu
    // Không trỏ trực tiếp tham chiếu bidderId đến đối tượng User để tránh việc truyền quá nhiều thông tin không cần thiết qua mạng lưới, chỉ cần lưu ID là đủ để xác định người đặt giá thầu khi cần thiết
    // Nếu cần có thể sửa lại để biến bidderId thành một đối tượng User, nhưng cần đảm bảo rằng khi truyền qua mạng lưới thì chỉ truyền ID của người đặt giá thầu thay vì toàn bộ thông tin của đối tượng User để tối ưu hóa hiệu suất và bảo mật thông tin
    private int auctionId; // ID của phiên đấu giá mà người dùng đang đặt giá thầu
    private double bidAmount;  // Số tiền của giá thầu
    private String timestamp; // thời gian khi được tạo ra -> dạng String 
    public BidTransaction() {
        super();
    }

    public BidTransaction(int auctionId, int bidderId, double bidAmount) {
        super();
        this.bidderId = bidderId;
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.timestamp = this.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    public int getBidderId() {
        return bidderId;
    }

    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }

    public int  getAuctionId() {
        return auctionId;
    }
    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }
    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }
    public String getTimeStamp() {
        return timestamp;
    }
}
