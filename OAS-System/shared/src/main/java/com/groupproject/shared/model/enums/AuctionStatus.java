package com.groupproject.shared.model.enums;

public enum AuctionStatus {
    WAITING, // Trạng thái chờ bắt đầu, người bán đã tạo đấu giá nhưng chưa bắt đầu
    ACTIVED, // Trạng thái đang diễn ra, người bán đã bắt đầu đấu giá và người mua có thể đặt giá thầu
    ENDED, // Trạng thái đã kết thúc, thời gian đấu giá đã hết nhưng chưa được xử lý để xác định người thắng cuộc hoặc hủy bỏ đấu giá
    CANCELLED, // Trạng thái đã hủy, người bán đã hủy bỏ đấu giá trước khi kết thúc, không còn khả năng đặt giá thầu hoặc xác định người thắng cuộc
    FINISHED // Trạng thái đã hoàn thành, đấu giá đã kết thúc và đã xác định người thắng cuộc, giao dịch đã được xử lý thành công
}
