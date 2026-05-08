package com.groupproject.client.utils;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneNavigator {
    // Một màn hình window duy nhất
    private static Stage mainStage;
    // Dùng 1 lần khi mở app
    public static void setMainStage(Stage stage) { mainStage = stage; }

    public static void goTo(String fxmlPath) {
        // Đặt trong Platform.runLater để đảm bảo an toàn khi gọi
        // Từ bất kỳ luồng nào, kể cả từ ServerListener chạy ở background
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
                Parent root = loader.load();
                
                mainStage.setScene(new Scene(root));
                mainStage.show();
                
            } catch (IOException e) {
                System.err.println("CRITICAL ERROR: Could not load FXML file -> " + fxmlPath);
                e.printStackTrace();
            }
        });
    }
}
