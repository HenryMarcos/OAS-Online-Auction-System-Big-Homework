package com.groupproject.client.utils;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneNavigator {

    private static SceneNavigator instance;

    // Một màn hình window duy nhất
    private static Stage mainStage;

    private SceneNavigator() {}

    public static SceneNavigator getInstance() {
        if (instance == null) { instance = new SceneNavigator(); }
        return instance;
    }

    // Dùng 1 lần khi mở app
    public void setMainStage(Stage stage) { mainStage = stage; }

    public void goTo(String fxmlPath) {
        // Đặt trong Platform.runLater để đảm bảo an toàn khi gọi
        // Từ bất kỳ luồng nào, kể cả từ ServerListener chạy ở background
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
                Parent root = loader.load();
                
                mainStage.setScene(new Scene(root, 1080, 720));
                mainStage.show();
                
            } catch (IOException e) {
                System.err.println("CRITICAL ERROR: Could not load FXML file -> " + fxmlPath);
                e.printStackTrace();
            }
        });
    }
}
