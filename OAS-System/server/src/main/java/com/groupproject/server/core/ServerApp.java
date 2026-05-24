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
        // 1. Khởi tạo database và nạp dữ liệu (Seed Data)
        DatabaseManager.getInstance().initDatabse();
        
        // 2. Tự động khởi tạo hệ thống đấu giá
        AuctionManager.getInstance();

        // ServerSocket chính là thứ lắng nghe lưu lượng truy cập internet
        try (ServerSocket serverSocket = new ServerSocket(Config.SERVER_PORT)) {
            ServerLogger.info("Server is online and listening on port " + Config.SERVER_PORT + "...");

            // Vòng lặp vô hạn để server tồn tại mãi mãi đợi clients
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ServerLogger.info("ServerApp: New client connected from " + clientSocket.getInetAddress());

                // Đưa client đến một luồng mới để server không bị đơ
                ClientHandler handler = new ClientHandler(clientSocket);
                clientThreadPool.execute(handler);
            }
        } catch (Exception e) {
            ServerLogger.error(e.getMessage());
        } finally {
            if (clientThreadPool != null) {
                clientThreadPool.shutdown();
            }
        }
    }

    // --- HÀM BÁO TIN/THÔNG BÁO ---
    public static void broadcast(String message, ObjectOutputStream senderOut) {
        // Lưu ý: Nên sử dụng ClientManager.getInstance().broadcastSystemEvent để đồng bộ với các logic trước đó
        synchronized (ClientManager.getInstance().getClients()) {
            for (ObjectOutputStream writer : ClientManager.getInstance().getClients()) {
                if (writer != senderOut) {
                    try {
                        writer.writeObject(message);
                        writer.flush();
                    } catch (Exception e) {
                        ServerLogger.error(e.getMessage());
                    }
                }
            }
        }
    }
}