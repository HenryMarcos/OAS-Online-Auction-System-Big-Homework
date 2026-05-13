package com.groupproject.shared.network;

public class NotificationResponse  {
    public enum NotificationType {
        INFO, // THÔNG BÁO CẬP NHẬT TRẠNG THÁI CỦA PHIÊN ĐẤU GIÁ 
        SUCCESS, // THÔNG BÁO CHO TOÀN BỘ KHI CÓ NGƯỜI THÀNH CÔNG ĐẶT GIÁ 
        ERROR // THÔNG BÁO CHO NGƯỜI ĐÓ NẾU ANH TA ĐẶT GIÁ THẤT BẠI
    }
}
