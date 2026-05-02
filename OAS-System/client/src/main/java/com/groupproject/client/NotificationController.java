package com.groupproject.client;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
public class NotificationController implements Initializable {
    @FXML
    private VBox notificationcontainer;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadNotifications();
    }
    private void loadNotifications() {
        //
    }

}
