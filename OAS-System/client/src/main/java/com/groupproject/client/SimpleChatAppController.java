package com.groupproject.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javafx.fxml.FXML;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class SimpleChatAppController {

    // Lấy textArea có id chatHistory từ file simpleChatApp.fxml
    @FXML
    private TextArea chatHistory;

    @FXML
    private TextField messageInput;

    // Mạng
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    // Hàm này tự động chạy khi màn hình chat được chọn
    @FXML
    public void initialize() {
        // Bắt đầu quá trình kết nối trong 1 luồng ở background để UI không bị đóng băng
        new Thread(() -> connectToServer()).start();
    }

    private void connectToServer() {
        try {
            String serverIp = "34.9.27.87";
            int port = 8080;

            System.out.println("Connecting to chat server...");
            socket = new Socket(serverIp, port);

            // Mở các đường dẫn (OuputStream phải được tạo trước InputStream)
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Connected! Starting listener...");

            // Vào vòng lặp nghe 
            listenForMessages();

        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            
            // Update UI để hiện lỗi
            Platform.runLater(() -> {
                chatHistory.appendText("--- ERROR: Could not connect to server ---\n");
                chatHistory.appendText("Reason: " + e.getMessage() + "\n");
            });
        }
    } 

    private void listenForMessages() {
        try {
            while (true) {
                // Luồng xử lý tạm dừng ở đây và chờ máy chủ gửi dữ liệu
                // (Giả sử máy chủ chỉ gửi String)
                String incomingMessage = (String) in.readObject();

                // Truyền dữ liệu này trở lại luồng UI JavaFX chính một cách an toàn
                // Dùng runlater cho các dữ liệu liên quan đến javafx ui
                Platform.runLater(() -> {
                    chatHistory.appendText("Them: " + incomingMessage + "\n");
                });
            }
        } catch (Exception e) {
            System.out.println("Disconnected from server.");
            Platform.runLater(() -> {
                chatHistory.appendText("--- Disconnected from server ---\n");
            });
        }
    }

    // Hàm này được kích hoạt khi bấm vào nút send hoặc ấn enter trong text field
    @FXML
    private void sendMessage() {
        // Lấy text của User nhập vào
        String text = messageInput.getText();

        // Kiểm tra xem có phải User chỉ gửi 1 khoảng trống không
        if (!text.trim().isEmpty()) {
            // Thêm text vào lịch sử chat
            chatHistory.appendText("Me: " + text + "\n");

            // Logic network
            // Gửi text cho server
            try {
                if (out != null) {
                    out.writeObject(text);
                    out.flush(); // Ép xuống đường dẫn
                }
            } catch (Exception e) {
                chatHistory.appendText("--- Failed to send message ---\n");
            }
        }

        // Dọn phần iput cũ đi để gửi tin nhắn mới
        messageInput.clear();
    }

}
