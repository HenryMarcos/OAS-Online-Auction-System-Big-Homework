package com.groupproject.shared.exception;

/**
 * Ngoại lệ ném ra khi mức giá người dùng đặt không hợp lệ 
 * (ví dụ: thấp hơn hoặc bằng giá hiện tại).
 */
public class InvalidBidException extends Exception {
    public InvalidBidException(String message) {
        super(message);
    }
}