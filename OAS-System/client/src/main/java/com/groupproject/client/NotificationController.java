package com.groupproject.client;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import com.groupproject.client.utils.NotificationStore;
import com.groupproject.shared.network.Notification;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
public class NotificationController implements Initializable {
    @FXML
    private VBox notificationcontainer;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadNotifications();
    }
    private void loadNotifications() {
        notificationcontainer.getChildren().clear();
        ObservableList<Notification> list= NotificationStore.getInstance().getNotification();
        try {
            for (Notification notification : list) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/groupproject/client/FXML/notificationitem.fxml"));
                HBox itemBox = loader.load();
                Label messageLabel= (Label) itemBox.lookup("#message");
                Label timeLabel =(Label) itemBox.lookup("#messageTime");
                messageLabel.setText(notification.getMessage());
                timeLabel.setText(notification.getTime());
                notificationcontainer.getChildren().add(itemBox);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}
