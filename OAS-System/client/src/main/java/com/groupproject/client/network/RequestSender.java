package com.groupproject.client.network;

import java.io.IOException;
import java.io.ObjectOutputStream;

import com.groupproject.client.utils.ClientLogger;
import com.groupproject.shared.network.Request;

public class RequestSender {

    public static void send(Request request) {
        ObjectOutputStream out = NetworkManager.getInstance().getOut();
        // Kiểm tra xem đã kết nối chưa
        if (out == null) {
            ClientLogger.error("CRITICAL ERROR: Trying to send data, but not connected to server!");
            return;
        }

        try {
            // Gửi yêu cầu
            out.writeObject(request);
            out.flush();
            out.reset(); // Dọn bộ nhớ

            // Thông báo đã gửi yêu cầu
            ClientLogger.info("-> Sent request: " + request.getClass().getSimpleName());

        } catch (IOException e) {
            ClientLogger.error("Failed to send request: " + request.getClass().getSimpleName());
            ClientLogger.error(e.getMessage());
        }
    }
}
