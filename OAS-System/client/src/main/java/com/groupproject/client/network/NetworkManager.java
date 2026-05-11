package com.groupproject.client.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
        System.out.println("Connecting to server at " + host + ":" + port + "...");
        socket = new Socket(host, port);
        
        // out trước in
        out = new ObjectOutputStream(socket.getOutputStream()); // Gửi cho server
        out.flush();
        in = new ObjectInputStream(socket.getInputStream()); // Nhận từ server

        System.out.println("Successfully connected to the server!");
    }

    public void disconnect() {
        try {
            if (in != null) { in.close(); }
            if (out != null) { out.close(); }
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Disconnected from server.");
        } catch (IOException e) {
            System.err.println("Error while disconnecting: " + e.getMessage());
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
