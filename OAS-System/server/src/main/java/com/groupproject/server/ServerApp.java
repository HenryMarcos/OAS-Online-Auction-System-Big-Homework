// Class chính để setup server và các thứ các thứ

package com.groupproject.server;



import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
                        log("Broadcast Request: " + message);

                        // Gửi thông báo cho tất cả mọi người
                        broadcast(message, out);
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
