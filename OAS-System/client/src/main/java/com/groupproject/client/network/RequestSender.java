package com.groupproject.client.network;

import java.io.IOException;
import java.io.ObjectOutputStream;

import com.groupproject.shared.network.NetworkRequest;
import com.groupproject.shared.network.Request;

public class RequestSender {
    private static ObjectOutputStream out;

    public static void initialize(ObjectOutputStream outputStream) {
        out = outputStream;
    }

    // The single, clean method that all your JavaFX Controllers will use
    public static void send(String action, Object payload) {
        // 1. Safety Check: Are we actually connected?
        if (out == null) {
            System.err.println("CRITICAL ERROR: Trying to send data, but not connected to server!");
            return;
        }

        try {
            // 2. Package the data into our standard envelope
            NetworkRequest request = new NetworkRequest(action, payload);

            // 3. Send it over the wire
            out.writeObject(request);
            out.flush();
            
            // 4. THE MAGIC LINE (Prevents a massive caching bug!)
            out.reset(); 

            System.out.println("-> Sent request to server: " + action);

        } catch (IOException e) {
            System.err.println("Failed to send request: " + action);
            e.printStackTrace();
        }
    }

    public static void send(Request request) {
        if (out == null) {
            System.err.println("CRITICAL ERROR: Trying to send data, but not connected to server!");
            return;
        }

        try {
            // Send the specific child object over the wire
            out.writeObject(request);
            out.flush();
            out.reset(); // Still keeping our magic cache-clearing line!

            // Log exactly which request we just sent (e.g., "-> Sent request: LoginRequest")
            System.out.println("-> Sent request: " + request.getClass().getSimpleName());

        } catch (IOException e) {
            System.err.println("Failed to send request: " + request.getClass().getSimpleName());
            e.printStackTrace();
        }
    }
}
