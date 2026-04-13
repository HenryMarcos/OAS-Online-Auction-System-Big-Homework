package com.groupproject.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.groupproject.shared.User;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    // Load file fxml
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    // Kiểm tra xem server và user có hoạt động trơn tru với nhau không 
    
    static void shootUserOverTheWire() {
        // localhost nghĩa là tìm server trong máy của chính mình
        String serverIp = "memory-exact.gl.at.ply.gg";
        int port = 9224;

        System.out.println("Attempting to connect to server...");

        // Mở kết nối đến cổng kết nỗi của máy chủ(cổng 8080)
        try (Socket socket = new Socket(serverIp, port)) {
            // Nếu kết nối được thì thông báo thành công
            System.out.println("Connected to the server");

            // Tạo 1 User để kiểm tra
            User myUser = new User("Henry", "e@gmail.com", "Ninooo");

            // Mở đường dẫn xử lý để gửi các đối tượng Java qua mạng
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            // Truyền dữ liệu qua đường truyền
            out.writeObject(myUser);

            // Lệnh flush buộc dữ liệu thực sự rời khỏi máy client và di chuyển trên đường truyền
            out.flush();
            } catch (Exception e) {
                // Báo lỗi nếu thất bại
                System.err.println("Could not connect to the server: " + e.getMessage());
                e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }

}