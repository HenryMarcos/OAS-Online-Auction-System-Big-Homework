package com.groupproject.shared.network.response;

/**
 * Phản hồi đặc biệt khi Client cố gắng truy cập tính năng yêu cầu đăng nhập
 * nhưng chưa thực hiện xác thực thành công.
 */
public class ErrorNotLoginResponse extends Response {
    private final String errorMessage;

    public ErrorNotLoginResponse() {
        // Mặc định thành công là false
        super(false); 
        this.errorMessage = "Yêu cầu bị từ chối: Bạn chưa đăng nhập vào hệ thống!";
    }

    public ErrorNotLoginResponse(String customMessage) {
        super(false);
        this.errorMessage = customMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}