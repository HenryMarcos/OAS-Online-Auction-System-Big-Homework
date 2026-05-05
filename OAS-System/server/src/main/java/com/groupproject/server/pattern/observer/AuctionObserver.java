package com.groupproject.server.pattern.observer;

/**
 * Interface đóng vai trò là "Radio" (Người quan sát)
 * Bất kỳ class nào muốn nhận thông báo về các sự kiện đấu giá (như khi có một mức giá mới được đặt) 
 * sẽ triển khai interface này và đăng ký với AuctionSubject để nhận cập nhật.
 */

public interface AuctionObserver {
    
    /**
     * 1. Khi có người đặt giá mới thành công.
     * Tương ứng với hàm placeBid() trong AuctionManager.
     */
    void onBidUpdated(String auctionId, double newBidAmount, String highestBidderId);

    /**
     * 2. Khi phiên đấu giá chính thức BẮT ĐẦU (Chuyển sang ACTIVED).
     * Báo cho Client biết để mở khóa nút "Đặt Giá" trên giao diện.
     * Tương ứng với hàm startAuction().
     */
    void onAuctionStarted(String auctionId);

    /**
     * 3. Khi phiên đấu giá ĐÃ HẾT GIỜ (Chuyển sang ENDED).
     * Báo cho Client biết để khóa nút "Đặt Giá" lại, hiện thông báo "Đang chốt kết quả...".
     * Tương ứng với hàm endAuction().
     */
    void onAuctionEnded(String auctionId);

    /**
     * 4. Khi phiên đấu giá HOÀN TẤT VÀ CHỐT GIAO DỊCH (Chuyển sang FINISHED).
     * Báo cho Client biết ai là người chiến thắng cuối cùng và giá chốt là bao nhiêu.
     * Tương ứng với hàm finishAuction().
     */
    void onAuctionFinished(String auctionId, String winnerId, double finalPrice);

    /**
     * 5. Khi phiên đấu giá BỊ HỦY GIỮA CHỪNG (Chuyển sang CANCELLED).
     * Báo cho Client biết phòng đã bị đóng do người bán hủy hoặc lỗi hệ thống, yêu cầu Client rời phòng.
     * Tương ứng với hàm cancelAuction().
     */
    void onAuctionCancelled(String auctionId, String reason);
}
