package com.groupproject.client.network;

import java.io.ObjectInputStream;

import com.groupproject.client.utils.ClientLogger;
import com.groupproject.shared.network.events.ServerEvent;
import com.groupproject.shared.network.responses.Response;

public class ServerListener implements Runnable {
    
    @Override
    public void run() {
        ObjectInputStream in = NetworkManager.INSTANCE.getIn();
        ClientLogger.info("Background listener started. Waiting for server...");
        try {
            while (true) { 
                // 1. This line WAITS until the server sends something
                Object incomingData = in.readObject();

                // 2. Make sure it's our standard response object
                if (incomingData instanceof Response || incomingData instanceof ServerEvent) {
                    ClientMessageRouter.INSTANCE.handleIncomingMessage(incomingData);
                } else {
                    ClientLogger.warning("Received unknown object from server.");
                }
            }
        } catch (Exception e) {
            // thông báo mất kêt nối với sever tại đây 
            // tạo thêm một cái show Allert 
            ClientLogger.error("Lost connection to the server.");
        }
    }
}