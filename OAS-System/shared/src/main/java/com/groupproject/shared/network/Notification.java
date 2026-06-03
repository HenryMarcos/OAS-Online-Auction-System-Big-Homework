package com.groupproject.shared.network;
// LỚP THÔNG BÁO ĐƯỢC DÙNG CHUNG Ở TRONG MÀN HÌNH NOTIFICATION

import java.io.Serializable;

public class Notification implements Serializable {
    private final String message;
    private final String time;
    private boolean isRead;
    public Notification(String message, String time) {
        this.message=message;
        this.time=time;
        this.isRead=false;

    }
    public String getMessage() {
        return message;
    }
    public void setIsRead(boolean isRead) {
        this.isRead= isRead;
    }
    public String getTime() {
        return time;
    }
    public boolean getIsRead() {
        return isRead;
    }
}
