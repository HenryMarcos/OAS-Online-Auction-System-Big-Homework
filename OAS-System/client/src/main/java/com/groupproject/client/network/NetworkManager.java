package com.groupproject.client.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.groupproject.client.utils.ClientLogger;

public class NetworkManager {
    private static NetworkManager instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private NetworkManager() {}

    public static NetworkManager getInstance() {
        if (instance == null) { instance = new NetworkManager(); }
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        ClientLogger.info("Connecting to server at " + host + ":" + port + "...");
        socket = new Socket(host, port);
        
        // out trước in
        out = new ObjectOutputStream(socket.getOutputStream()); // Gửi cho server
        out.flush();
        in = new ObjectInputStream(socket.getInputStream()); // Nhận từ server

        ClientLogger.info("Successfully connected to the server!");
    }

    public void disconnect() {
        try {
            if (in != null) { in.close(); }
            if (out != null) { out.close(); }
            if (socket != null && !socket.isClosed()) socket.close();
            ClientLogger.info("Disconnected from server.");
        } catch (IOException e) {
            ClientLogger.error("Error while disconnecting: " + e.getMessage());
        }
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    public ObjectInputStream getIn() {
        return in;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }
}
