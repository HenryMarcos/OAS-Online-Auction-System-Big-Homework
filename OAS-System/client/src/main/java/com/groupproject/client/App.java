package com.groupproject.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.groupproject.client.network.RequestSender;
import com.groupproject.client.network.ServerListener;
import com.groupproject.client.utils.SceneNavigator;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    // Các biến tĩnh để dùng chung cho toàn bộ app
    public static Socket socket;
    public static ObjectInputStream in;
    public static ObjectOutputStream out;

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
            socket = new Socket("34.9.27.87", 8080); 
            out = new ObjectOutputStream(socket.getOutputStream()); // Gửi cho server
            out.flush();
            in = new ObjectInputStream(socket.getInputStream()); // Nhận từ server
            RequestSender.initialize(out);

            ServerListener listener = new ServerListener(in);
            Thread listenerThread = new Thread(listener);
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