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
    private static final List<ObjectOutputStream> clientWriters = new ArrayList<>();

    private static final String DB_URL = "jbdc:sqlite:users.db";

    public static void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
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
                         "id INTERGER PRIMARY KEY AUTOINCREMENT," + 
                         "username TEXT UNIQUE NOT NULL," + 
                         "password TEXT NOT NULL)";
            
            stmt.execute(sql);
            System.out.println("Database initialized successfully!");
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // --- HÀM BÁO TIN/THÔNG BÁO ---
    // Gửi 1 tin nhắn cho mỗi client trong danh sách
    private static void broadcast(String message, ObjectOutputStream senderOut) {
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

    private static void handleAuth(String message, ObjectOutputStream senderOut) throws IOException {
        // Phân chia thông tin gửi lên thành 4 phần
        String[] parts = message.split(":");
        // Nếu không đủ 4 phần thì không hợp lệ
        if (parts.length < 4) return;

        String action = parts[1]; // Kiểm tra xem đây là LOGIN hay SIGNUP
        String username = parts[2]; // Tên người dùng
        String password = parts[3]; // Mật khẩu

        if (action.equals("LOGIN")) {
            boolean success = checkUser(username, password);
            senderOut.writeObject(success ? "SERVER:AUTH_SUCCESS" : "SERVER:AUTH_FAIL:Wrong username or password!");


        } else if (action.equals("SIGNUP")) {
            boolean success = saveUser(username, password);
            senderOut.writeObject(success ? "SERVER:AUTH_SUCCESS" : "SERVER:AUTH_FAIL:Username already exists!");
        } else {

        }

        senderOut.flush();
    }

    private static synchronized boolean saveUser(String username, String password) {
        // Câu lệnh sql để chèn user mới
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate(); // Chạy câu lệnh

            return true; // Đăng ký thành công

        } catch (Exception e) {
            // Sẽ văng lỗi nếu username đã tồn tại (do dính ràng buộc UNIQUE)
            return false;
        }
    }

    private static synchronized boolean checkUser(String username, String password) {
        // Câu lệnh SQL tìm user có username và password khớp
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, username);

            ResultSet rs = pstmt.executeQuery();
            
            // Nếu rs.next() là true, nghĩa là tìm thấy ít nhất 1 dòng khớp -> Đăng nhập thành công
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    
    // --- NỘI HÀM: CHUÕI RIÊNG CHO MỖI CLIENT ---
    public static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Mở đường dẫn (out trước in sau)
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                // Thêm client vào danh sách báo tin chính 1 cách an toàn 
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                // Vòng lặp vô hạn riêng cho client này
                while (true) {
                    Object recievedData = in.readObject();

                    if (recievedData instanceof String) {
                        String message = (String) recievedData;
                        // Kiểm tra yêu cầu xác thực
                        // Khi gửi thông tin xác thực 1 user đã có tài khoản sẽ gửi dưới dạng
                        // AUTH:LOGIN:username:password
                        // Nếu đăng nhập và
                        // AUTH:SIGNUP:username:password
                        // Nếu đăng ký
                        if (message.startsWith("AUTH:")) {
                            handleAuth(message, out);
                        } else {
                            log("Broadcast Request: " + message);

                            // Gửi thông báo cho tất cả mọi người
                            broadcast(message, out);
                        }
                    }
                }


            } catch (Exception e) {
                log("A client disconnected");
            } finally {
                // CLEANUP: Khi client rời, xóa client trong danh sách đi
                if (out != null) {
                    synchronized (clientWriters) {
                        clientWriters.remove(out);
                    }
                }
                try { socket.close(); } catch (Exception e) {}
            }
        }
    }

}
