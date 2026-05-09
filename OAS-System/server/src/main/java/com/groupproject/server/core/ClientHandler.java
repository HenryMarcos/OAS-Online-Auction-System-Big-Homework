package com.groupproject.server.core;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import com.groupproject.server.dao.CategoryDAO;
import com.groupproject.server.handlers.RequestDispatcher;
import com.groupproject.server.service.BidHandler;
import com.groupproject.shared.network.BidRequest;
import com.groupproject.shared.network.NetworkRequest;
import com.groupproject.shared.network.Request;
import com.groupproject.shared.network.Response;

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

            // Thêm client vào danh sách báo tin chính 1 cách an toàn 
            synchronized (ServerApp.clientWriters) {
                ServerApp.clientWriters.add(out);
            }

            RequestDispatcher dispatcher = new RequestDispatcher();

            // Vòng lặp vô hạn riêng cho client này
            while (true) {
                // Kiểm tra xem client gửi gì
                Object recievedData = in.readObject();

                // Xử lý trường hợp client gửi yêu cầu
                if (recievedData instanceof Request) {
                    Request request = (Request) recievedData;
                    // Nhận response sau khi xử lý xong request
                    Response serverReply = dispatcher.dispatch(request);

                    if (serverReply != null) {
                        out.writeObject(serverReply);
                        out.flush();
                        out.reset();
                    }
                }
                else if (recievedData instanceof NetworkRequest) {
                    NetworkRequest networkRequest = (NetworkRequest) recievedData;

                    if (networkRequest.getAction().equals("GET_CATEGORY_FIELDS")) {

                        int categoryId = (int) networkRequest.getPayload();

                        List<String> requiredFields = CategoryDAO.getRequiredFieldsForCategory(categoryId);
                        out.writeObject(requiredFields);
                        out.flush();
                        
                        System.out.println("Sent " + requiredFields.size() + " fields to the client.");
                    }
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