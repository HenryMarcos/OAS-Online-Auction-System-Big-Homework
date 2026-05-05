package com.groupproject.shared.exception;

/**
 * Ngoại lệ ném ra khi hệ thống không tìm thấy phiên đấu giá
 * (có thể do ID không tồn tại hoặc phiên đã bị xóa khỏi bộ nhớ).
 */
public class AuctionNotFoundException extends Exception {
    public AuctionNotFoundException(String message) {
        super(message);
    }
}