package com.groupproject.client;

import java.io.IOException;

import com.groupproject.client.network.NetworkManager;
import com.groupproject.client.network.ServerListener;
import com.groupproject.client.utils.ClientConfig;
import com.groupproject.client.utils.SceneNavigator;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    // Các biến tĩnh để dùng chung cho toàn bộ app
    private static Scene scene;

    

    @Override
    public void start(Stage stage) throws IOException {
        // Kết nối với server 1 lần duy nhất khi bắt đầu
        new Thread(() -> connectToServer()).start();

        SceneNavigator.getInstance().setMainStage(stage);
        // Vào màn hình login
        SceneNavigator.getInstance().goTo("/com/groupproject/client/FXML/login.fxml");
    }

    public static void connectToServer() {
        try {
            // Dùng Google Cloud IP hoặc bất kỳ IP nào phù hợp
            NetworkManager.getInstance().connect(ClientConfig.getServerIp(), ClientConfig.getServerPort());
                        
            Thread listenerThread = new Thread(new ServerListener());
            listenerThread.setDaemon(true); // Đảm bảo thread sẽ chết khi tắt app
            listenerThread.start();

            System.out.println("Connected to server for login");
        } catch (Exception e) {
            System.out.println("Could not connect to server for login.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }

}