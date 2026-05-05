package com.groupproject.shared.exception;

/**
 * Ngoại lệ ném ra khi người dùng chưa đăng nhập, hoặc thông tin
 * định danh (bidderId) bị trống/không hợp lệ khi thực hiện giao dịch.
 */
public class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }
}