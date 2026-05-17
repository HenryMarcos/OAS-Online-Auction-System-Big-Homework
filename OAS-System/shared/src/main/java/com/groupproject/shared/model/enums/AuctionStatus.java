package com.groupproject.shared.model.enums;

// shared/model/enums/AuctionStatus.java
public enum AuctionStatus {

    //        Text hiển thị     Cho phép bid?
    WAITING  ("Chờ bắt đầu",   false), // Trạng thái chờ bắt đầu, người bán đã tạo đấu giá nhưng chưa bắt đầu
    ACTIVED  ("Đang diễn ra",  true),   // Trạng thái đang diễn ra, người bán đã bắt đầu đấu giá và người mua có thể đặt giá thầu
    ENDED    ("Hết giờ",       false),  // Trạng thái đã kết thúc, thời gian đấu giá đã hết nhưng chưa được xử lý để xác định người thắng cuộc hoặc hủy bỏ đấu giá
    FINISHED ("Đã hoàn thành", false),  // Trạng thái đã hoàn thành, đấu giá đã kết thúc và đã xác định người thắng cuộc, giao dịch đã được xử lý thành công
    CANCELLED("Đã hủy",        false);  // Trạng thái đã hủy, người bán đã hủy bỏ đấu giá trước khi kết thúc, không còn khả năng đặt giá thầu hoặc xác định người thắng cuộc

    private final String displayText;
    private final boolean canBid;

    AuctionStatus(String displayText, boolean canBid) {
        this.displayText = displayText;
        this.canBid = canBid;
    }

    public String getDisplayText() { return displayText; }
    public boolean canBid()        { return canBid; }
}
