package com.groupproject.shared.network.event;

import java.time.LocalDateTime;

/**
 * Event dùng để gửi thông báo từ hệ thống hoặc Admin tới tất cả người dùng
 */
public class SystemNotificationEvent extends ServerEvent {
    private String message;
    private String senderName; // Thường để là "Hệ thống" hoặc tên Admin
    private LocalDateTime timestamp;

    public SystemNotificationEvent(String message, String senderName) {
        this.message = message;
        this.senderName = senderName;
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getMessage() { return message; }
    public String getSenderName() { return senderName; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "[" + senderName + "]: " + message;
    }
}