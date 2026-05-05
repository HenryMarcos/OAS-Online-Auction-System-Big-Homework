package com.groupproject.shared.exception;

/**
 * Ngoại lệ ném ra khi cố gắng thực hiện một hành động không phù hợp
 * với trạng thái hiện tại của vòng đời đấu giá (Lifecycle).
 * Ví dụ: Bắt đầu một phiên đã kết thúc, hoặc Hủy một phiên đã hoàn thành.
 */
public class InvalidAuctionStateException extends Exception {
    public InvalidAuctionStateException(String message) {
        super(message);
    }
}