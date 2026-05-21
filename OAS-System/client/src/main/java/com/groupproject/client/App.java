package com.groupproject.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.groupproject.client.network.RequestSender;
import com.groupproject.client.network.ServerListener;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.shared.model.user.User;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

        // Vào màn hình login
        //scene = new Scene(loadFXML("login"), 640, 480);
        SceneNavigator.setMainStage(stage);
        
        // 2. Chuyển đến màn hình đầu tiên (ví dụ: màn hình Đăng nhập)
        SceneNavigator.goTo("/com/groupproject/client/FXML/login.fxml");
        
        // 3. Cài đặt tiêu đề và hiển thị (nếu goTo chưa show)
        stage.setTitle("Hệ thống đấu giá OAS");
        
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }
    public static void connectToServer() {
        try {
            // Use your Google Cloud IP
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