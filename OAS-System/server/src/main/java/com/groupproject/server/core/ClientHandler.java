package com.groupproject.server.core;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.groupproject.server.handlers.RequestDispatcher;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.requests.LoginRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.requests.SignupRequest;
import com.groupproject.shared.network.responses.Response;

// --- NỘI HÀM: CHUÕI RIÊNG CHO MỖI CLIENT ---
public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream in;   
    private ObjectOutputStream out;

    private Integer authenticatedUserId = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Mở đường dẫn (out trước in sau)
            out = new ObjectOutputStream(socket.getOutputStream()); // Từ server đến client
            out.flush();
            in = new ObjectInputStream(socket.getInputStream()); // Từ client đến server

            // Thêm client vào danh sách báo tin chính 1 cách an toàn 
            ClientManager.INSTANCE.addClient(out);
            RequestDispatcher dispatcher = new RequestDispatcher();

            // Vòng lặp vô hạn riêng cho client này
            while (true) {
                // Kiểm tra xem client gửi gì
                Object recievedData = in.readObject();

                // Xử lý trường hợp client gửi yêu cầu
                // Có 5 yêu cầu: CreateAuctionRequest, LoginRequest, SignupRequest, ChangeAuctionStatusHandle, GetMyAuctionHandler
                if (recievedData instanceof Request) {
                    Request request = (Request) recievedData;
                    ServerLogger.info("User " + socket.getInetAddress() + " sent a " + request.getClass().getSimpleName());
                    // Nhận response sau khi xử lý xong request
                    Response serverReply = dispatcher.dispatch(request, this);

                    if (serverReply != null) {
                        out.writeObject(serverReply);
                        out.flush();
                        out.reset();
                    }
                }
            }
        } catch (Throwable e) { // <-- Catch EVERYTHING
            ServerLogger.error("Client disconnected or error occurred: " + e.getMessage());

        } finally {
            // CLEANUP: Khi client rời, xóa client trong danh sách các client và các phòng đấu giá
            if (out != null) {
                ClientManager.INSTANCE.removeClient(out);
            }

            if (authenticatedUserId != null) {
                ClientManager.INSTANCE.unregisterUser(authenticatedUserId);
            }
            try { 
                if (socket != null && !socket.isClosed()) {
                    ServerLogger.info("Cleaning up connection for " + socket.getInetAddress());
                    socket.close(); 
                }
            } catch (Exception e) {
                ServerLogger.error("Error closing socket: " + e.getMessage());
            }
        }
    }

    public void setAuthenticatedUserId(Integer userId) { this.authenticatedUserId = userId; }
    public Integer getAuthenticatedUserId() { return authenticatedUserId; }

    public ObjectOutputStream getOut() { return out; }
}