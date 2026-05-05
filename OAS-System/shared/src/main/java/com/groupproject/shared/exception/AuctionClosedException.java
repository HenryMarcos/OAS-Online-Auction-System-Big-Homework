package com.groupproject.shared.exception;

/**
 * Ngoại lệ ném ra khi người dùng cố gắng tương tác với một phiên đấu giá
 * không ở trạng thái HOẠT ĐỘNG (ví dụ: đã kết thúc hoặc chưa bắt đầu).
 */
public class AuctionClosedException extends Exception {
    public AuctionClosedException(String message) {
        super(message);
    }
}