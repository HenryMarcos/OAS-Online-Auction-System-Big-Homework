package com.groupproject.client.network;

import java.io.ObjectInputStream;

import com.groupproject.shared.network.Response;

public class ServerListener implements Runnable {

    private ObjectInputStream in;

    public ServerListener(ObjectInputStream in) {
        this.in = in;
    }
    
    @Override
    public void run() {
        System.out.println("Background listener started. Waiting for server...");
        try {
            while (true) { 
                // 1. This line WAITS until the server sends something
                Object incomingData = in.readObject();

                // 2. Make sure it's our standard response object
                if (incomingData instanceof Response) {
                    Response response = (Response) incomingData;

                    EventRouter.getInstance().dispatch(response);
                } else {
                    System.out.println("Received unknown object from server.");
                }
            }
        } catch (Exception e) {
            // TODO: xử lý mất kết nối
        }
    }
}