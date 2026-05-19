package com.groupproject.client.utils;
import com.groupproject.shared.network.Notification;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
public class NotificationStore {
    private static final NotificationStore instance = new NotificationStore();
    private final ObservableList<Notification> notifications= FXCollections.observableArrayList();
    private final IntegerProperty unreadCount= new SimpleIntegerProperty(0);
    public static NotificationStore getInstance() {
        return instance;
    }
    public void addNotification(Notification notify) {
        Platform.runLater(() -> {
                notifications.add(0,notify);
                unreadCount.set(unreadCount.get()+1);
        });
    }
    public void markAllReads() {
        unreadCount.set(0);
    }
    public ObservableList<Notification> getNotification() {
        return notifications;
    }
    public IntegerProperty unreadCountProperty() {
        return unreadCount;
    }
}
