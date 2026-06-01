package com.groupproject.shared.model.transaction;

import java.io.Serializable;

public class NotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String message;
    private boolean isRead;

    public NotificationDTO(int id, String message, boolean isRead) {
        this.id = id;
        this.message = message;
        this.isRead = isRead;
    }

    public int getId() { return id; }
    public String getMessage() { return message; }
    public boolean isRead() { return isRead; }
    
    // So it displays nicely in a JavaFX ListView
    @Override
    public String toString() {
        return (isRead ? "[Read] " : "[NEW] ") + message; 
    }
}
