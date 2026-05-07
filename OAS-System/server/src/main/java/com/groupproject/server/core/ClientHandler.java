package com.groupproject.server.core;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.groupproject.server.service.AuthHandler;
import com.groupproject.server.service.AuthHandlerFactory;
import com.groupproject.server.service.BidHandler;
import com.groupproject.shared.AuthRequest;
import com.groupproject.shared.BidRequest;

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
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            // Thêm client vào danh sách báo tin chính 1 cách an toàn 
            synchronized (ServerApp.clientWriters) {
                ServerApp.clientWriters.add(out);
            }

            // Vòng lặp vô hạn riêng cho client này
            while (true) {
                
                Object recievedData = in.readObject();

                if (recievedData instanceof AuthRequest) {
                    AuthRequest request = (AuthRequest) recievedData;

                    AuthHandler handler = AuthHandlerFactory.getHandler(request.getType());

                    handler.handle(request, out);
                }
                else if (recievedData instanceof BidRequest) {
                    BidRequest bidRequest = (BidRequest) recievedData;

                    new BidHandler().handle(bidRequest, out);
                }
                else if (recievedData instanceof String) {
                    String message = (String) recievedData;
            
                    ServerApp.log("Broadcast Request: " + message);

                    // Gửi thông báo cho tất cả mọi người
                    ServerApp.broadcast(message, out);
                }
            }


        } catch (Exception e) {
            ServerApp.log("A client disconnected");
            e.printStackTrace();
        } finally {
            // CLEANUP: Khi client rời, xóa client trong danh sách đi
            if (out != null) {
                synchronized (ServerApp.clientWriters) {
                    ServerApp.clientWriters.remove(out);
                }
            }
            try { socket.close(); } catch (Exception e) {}
        }
    }
}