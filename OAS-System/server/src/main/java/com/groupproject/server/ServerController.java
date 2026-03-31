package com.groupproject.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import com.groupproject.shared.User;

public class ServerController {
    @FXML
    private TextArea serverLogs;

    // Danh sách tổng hợp tất cả các đường dẫn output của các máy client được kết nối
    private final List<ObjectOutputStream> clientWriters = new ArrayList<>();

    // Chương trình này sẽ tự động chạy khi UI server được mở
    @FXML
    public void initialize() {
        log("Server Application Started.");
        
        // Chuyển vòng lặp lắng nghe mạng nặng sang một luồng nền
        new Thread(() -> runServerLoop()).start();
    }

    // Giúp in văn bản một cách an toàn ra UI từ bất kỳ luồng nào.
    private void log(String message) {
        Platform.runLater(() -> {
            serverLogs.appendText(message + "\n");
        });
    }

    private void runServerLoop() {
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
    private void boardcast(String message) {
        synchronized (clientWriters) {
            for (ObjectOutputStream writer : clientWriters) {
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

    // --- NỘI HÀM: CHUÕI RIÊNG CHO MỖI CLIENT ---
    private class ClientHandler implements Runnable {
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
                        log("Boardcast Request: " + message);

                        // Gửi thông báo cho tất cả mọi người
                        boardcast(message);
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
