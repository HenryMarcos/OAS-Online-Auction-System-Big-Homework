package com.groupproject.server.core;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.groupproject.server.handlers.RequestDispatcher;
import com.groupproject.server.utils.ClientContext;
import com.groupproject.server.utils.ServerLogger;
import com.groupproject.shared.network.event.SystemNotificationEvent;
import com.groupproject.shared.network.request.Request;
import com.groupproject.shared.network.response.Response;

// --- NỘI HÀM: CHUÕI RIÊNG CHO MỖI CLIENT ---
public class ClientHandler implements Runnable {
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
            out = new ObjectOutputStream(socket.getOutputStream()); // Từ server đến client
            out.flush();
            in = new ObjectInputStream(socket.getInputStream()); // Từ client đến server

            // Lưu ObjectOutputStream của client này vào ThreadLocal để các phần khác của code có thể truy cập dễ dàng
            ClientContext.currentOut.set(out);

            // Thêm client vào danh sách báo tin chính 1 cách an toàn 
            ClientManager.getInstance().addClient(out);
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
                    Response serverReply = dispatcher.dispatch(request);

                    if (serverReply != null) {
                        out.writeObject(serverReply);
                        out.flush();
                        out.reset();
                    }
                } else if (recievedData instanceof String) {
                    String message = (String) recievedData;
    
                    // 1. Lấy thông tin user hiện tại từ ThreadLocal thông qua ClientContext
                    var currentUser = ClientContext.currentUser.get();

                    // 2. Kiểm tra xem người dùng có phải là Admin hay không
                    if (currentUser != null && currentUser.isAdmin()) {
                        ServerLogger.info("Admin [" + currentUser.getUsername() + "] phát thông báo hệ thống: " + message);
                        
                        // 3. Tạo một ServerEvent để bọc tin nhắn (Giúp Client dễ dàng phân loại và hiển thị)
                        // Lưu ý: Bạn nên có một class SystemNotificationEvent kế thừa ServerEvent
                        SystemNotificationEvent notification = new SystemNotificationEvent(message, "Hệ Thống");

                        // 4. Gọi hàm broadcastSystemEvent từ ClientManager để gửi cho tất cả mọi người
                        ClientManager.getInstance().broadcastSystemEvent(notification);
                        
                    } else {
                        // Xử lý trường hợp User bình thường cố tình gửi tin nhắn broadcast
                        String actor = (currentUser != null) ? currentUser.getUsername() : "Ẩn danh";
                        ServerLogger.warning("Cảnh báo: Người dùng [" + actor + "] cố gắng dùng quyền Admin trái phép.");
                        
                        // (Tùy chọn) Gửi thông báo lỗi ngược lại cho người gửi
                        // sendToClient(new ErrorResponse("Bạn không có quyền phát thông báo toàn hệ thống!"));
                    }
                }
            }


        } catch (Throwable e) { // <-- Catch EVERYTHING
            /* 
            // 1. PRINT THE ERROR FIRST before doing anything else!
            System.err.println("============== SERVER THREAD CRASHED ==============");
            e.printStackTrace(); 
            System.err.println("===================================================");
            
            // 2. Safely attempt to log it (Wrap in Platform.runLater if it touches UI)
            try {
                javafx.application.Platform.runLater(() -> {
                    ServerApp.log("A client disconnected due to an error.");
                });
            } catch (Exception logEx) {
                // Ignore if UI logging fails
            }
            */

            ServerLogger.error("Client disconnected or error occurred: " + e.getMessage());

        } finally {
            // CLEANUP: Khi client rời, xóa client trong danh sách các client và các phòng đấu giá
            if (out != null) {
                ClientManager.getInstance().removeClientCompletely(out);
            }
            ClientContext.clear(); // Dọn dẹp ThreadLocal
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
}