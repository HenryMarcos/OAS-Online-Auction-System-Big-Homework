// Class chính để setup server và các thứ các thứ

package com.groupproject.server.core;



import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.groupproject.server.dao.DatabaseManager;
import com.groupproject.server.utils.Config;

public class ServerApp {

    /* 
    private static Scene scene;

    @Override
    public void start(Stage stage) throws Exception {
        scene = new Scene(loadFXML("server"), 640, 480);

        stage.setTitle("OAS-App Server Console");
        stage.setScene(scene);

        // Đảm bảo luồng server nền sẽ dừng lại khi nhấn nút x để đóng cửa sổ
        stage.setOnCloseRequest(event -> System.exit(0));

        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    // Load file fxml
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ServerApp.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    */

    // Danh sách tổng hợp tất cả các đường dẫn output của các máy client được kết nối
    public static final List<ObjectOutputStream> clientWriters = new ArrayList<>();

    public static void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        // Khởi tạo database người dùng
        DatabaseManager.initDatabse();

        // launch(args);

        // ServerSocket chính là thứ lắng nghe lưu lượng truy cập internet
        try (ServerSocket serverSocket = new ServerSocket(Config.SERVER_PORT)) {
            // Thông báo để kiểm tra xem server có chạy được hay không
            System.out.println("Server is online and listening on port " + Config.SERVER_PORT + "...");

            // Vòng lặp vô hạn để server tồn tại mãi mãi đợi clients
            while (true) {
                // Đợi đến khi có 1 client kết nối tới server
                // Code sẽ dừng ở dòng này và chỉ chạy tiếp khi có client kết nối
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from " + clientSocket.getInetAddress());

                // Đưa client đến một luồng mới để server không bị đơ
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            // Dừng vòng lặp lại tại đây
            log("Server Error: " + e.getMessage());
        }
    }
    // --- HÀM BÁO TIN/THÔNG BÁO ---
    // Gửi 1 tin nhắn cho mỗi client trong danh sách
    public static void broadcast(String message, ObjectOutputStream senderOut) {
        synchronized (clientWriters) {
            for (ObjectOutputStream writer : clientWriters) {
                if (writer != senderOut) {
                    try {
                        writer.writeObject(message);
                        writer.flush();
                    } catch (Exception e) {
                        // Nếu có lỗi thì bỏ qua
                        // Hàm catch sẽ xử ly vẫn đề chết kết nối
                    }
                }
            }
        }
    }

    

}
