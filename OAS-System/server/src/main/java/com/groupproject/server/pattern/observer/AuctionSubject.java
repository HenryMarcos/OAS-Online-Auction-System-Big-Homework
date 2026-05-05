package com.groupproject.server.pattern.observer;

/**
 * Interface đóng vai trò là "Cái Micro" (Chủ thể phát sóng).
 * Class quản lý đấu giá (như AuctionManager) sẽ implement interface này 
 * để cho phép người khác đăng ký và phát thông báo cho họ.
 */
public interface AuctionSubject {
    
    /**
     * Đăng ký một người quan sát mới (Cho phép người mới bật radio dò đúng tần số).
     */
    void addObserver(String auctionId, AuctionObserver observer);

    /**
     * Hủy đăng ký một người quan sát (Người dùng tắt radio, không nghe nữa).
     */
    void removeObserver(String auctionId, AuctionObserver observer);

    /**
     * Các phương thức thông báo (Phát sóng đến tất cả những ai đang nghe radio).
     * 5 hàm phát thông báo tương ứng với 5 sự kiện của Observer
     */
    void notifyBidUpdated(String auctionId, double newBidAmount, String highestBidderId);
    void notifyAuctionStarted(String auctionId);
    void notifyAuctionEnded(String auctionId);
    void notifyAuctionFinished(String auctionId, String winnerId, double finalPrice);
    void notifyAuctionCancelled(String auctionId, String reason);
}