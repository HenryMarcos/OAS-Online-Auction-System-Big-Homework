// Class chính để setup server và các thứ các thứ

package com.groupproject.server.core;



import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.groupproject.server.dao.DatabaseManager;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.server.utils.Config;
import com.groupproject.server.utils.ServerLogger;

public class ServerApp {

    private static int MAX_CONCURRENT_USER = 50;
    private static final ExecutorService clientThreadPool = Executors.newFixedThreadPool(MAX_CONCURRENT_USER);

    public static void main(String[] args) {
        // Khởi tạo database người dùng
        DatabaseManager.INSTANCE.initDatabse();
        AuctionManager.INSTANCE.refreshCache();

        // ServerSocket chính là thứ lắng nghe lưu lượng truy cập internet
        try (ServerSocket serverSocket = new ServerSocket(Config.SERVER_PORT)) {
            // Thông báo để kiểm tra xem server có chạy được hay không
            ServerLogger.info("Server is online and listening on port " + Config.SERVER_PORT + "...");

            // Vòng lặp vô hạn để server tồn tại mãi mãi đợi clients
            while (true) {
                // Đợi đến khi có 1 client kết nối tới server
                // Code sẽ dừng ở dòng này và chỉ chạy tiếp khi có client kết nối
                Socket clientSocket = serverSocket.accept();
                ServerLogger.info("ServerApp: New client connected from " + clientSocket.getInetAddress());

                // Đưa client đến một luồng mới để server không bị đơ
                ClientHandler handler = new ClientHandler(clientSocket);
                clientThreadPool.execute(handler);
            }
        } catch (Exception e) {
            // Dừng vòng lặp lại tại đây
            ServerLogger.error(e.getMessage());
        } finally {
            // Dọn dẹp pool nếu server shutdown
            if (clientThreadPool != null) {
                clientThreadPool.shutdown();
            }
        }
    }
    // --- HÀM BÁO TIN/THÔNG BÁO ---
    // Gửi 1 tin nhắn cho mỗi client trong danh sách
    public static void broadcast(String message, ObjectOutputStream senderOut) {
        synchronized (ClientManager.INSTANCE.getClients()) {
            for (ObjectOutputStream writer : ClientManager.INSTANCE.getClients()) {
                if (writer != senderOut) {
                    try {
                        writer.writeObject(message);
                        writer.flush();
                    } catch (Exception e) {
                        // Nếu có lỗi thì bỏ qua
                        // Hàm catch sẽ xử lý vẫn đề chết kết nối
                        ServerLogger.error(e.getMessage());
                    }
                }
            }
        }
    }

}
