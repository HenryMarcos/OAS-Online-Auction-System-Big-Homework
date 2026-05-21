package com.groupproject.shared.model.transaction;

import com.groupproject.shared.model.base.Entity;

public class BidTransaction extends Entity {
    private static final long serialVersionUID = 1L;

    private Integer bidderId; // ID của người đặt giá thầu
    // Không trỏ trực tiếp tham chiếu bidderId đến đối tượng User để tránh việc truyền quá nhiều thông tin không cần thiết qua mạng lưới, chỉ cần lưu ID là đủ để xác định người đặt giá thầu khi cần thiết
    // Nếu cần có thể sửa lại để biến bidderId thành một đối tượng User, nhưng cần đảm bảo rằng khi truyền qua mạng lưới thì chỉ truyền ID của người đặt giá thầu thay vì toàn bộ thông tin của đối tượng User để tối ưu hóa hiệu suất và bảo mật thông tin
    private Integer auctionId; // ID của phiên đấu giá mà người dùng đang đặt giá thầu
    private double bidAmount; // Số tiền của giá thầu

    public BidTransaction() {
        super();
    }

    public BidTransaction(int bidderId, int auctionId, double bidAmount) {
        super();
        this.bidderId = bidderId;
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
    }
    
    public Integer getBidderId() {
        return bidderId;
    }

    public void setBidderId(Integer bidderId) {
        this.bidderId = bidderId;
    }

    public Integer getAuctionId() {
        return auctionId;
    }
    public void setAuctionId(Integer auctionId) {
        this.auctionId = auctionId;
    }
    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }
}
