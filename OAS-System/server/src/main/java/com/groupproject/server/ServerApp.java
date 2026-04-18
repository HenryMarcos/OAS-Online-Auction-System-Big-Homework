// Class chính để setup server và các thứ các thứ

package com.groupproject.server;



import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.groupproject.server.Authentication.AuthHandler;
import com.groupproject.server.Authentication.AuthHandlerFactory;
import com.groupproject.shared.AuthRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

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

    private static final String DB_URL = "jdbc:sqlite:users.db";

    public static void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        // Khởi tạo database người dùng
        initDatabse();

        // launch(args);
        int port = 8080; // Cổng mà server sẽ lắng nghe

        // ServerSocket chính là thứ lắng nghe lưu lượng truy cập internet
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Thông báo để kiểm tra xem server có chạy được hay không
            System.out.println("Server is online and listening on port " + port + "...");

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

    public static void initDatabse() {
        try (Connection conn = DriverManager.getConnection(DB_URL); 
             Statement stmt = conn.createStatement()) {

            // Tạo bảng users nếu chưa tồn tại 
            String sql = "CREATE TABLE IF NOT EXISTS users (" + 
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," + 
                         "username TEXT UNIQUE NOT NULL," + 
                         "email TEXT UNIQUE NOT NULL," +
                         "password TEXT NOT NULL)";
            
            stmt.execute(sql);
            System.out.println("Database initialized successfully!");
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
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
