package com.groupproject.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    
    // NOTE: You must pass your connected Socket/Streams to this controller 
    // before making these calls, just like your Chat controller!
    private ObjectOutputStream out;
    private ObjectInputStream in;

    @FXML
    private void handleLogin() {
        sendAuthRequest("LOGIN");
    }

    @FXML
    private void handleSignUp() {
        sendAuthRequest("SIGNUP");
    }

    public void initialize() {
        new Thread(() -> connectToServer()).start();
    }

    private void connectToServer() {
        try {
            // Use your Google Cloud IP
            Socket socket = new Socket("34.9.27.87", 8080); 
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.out.println("Could not connect to server for login.");
        }
    }

    private void sendAuthRequest(String action) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both fields.");
            return;
        }

        try {
            // Send the formatted string
            out.writeObject("AUTH:" + action + ":" + username + ":" + password);
            out.flush();

            // Wait for the server's response
            String response = (String) in.readObject();
            
            if (response.equals("SERVER:AUTH_SUCCESS")) {
                statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                statusLabel.setText("Success! Loading chat...");
                // TODO: Switch scene to your Chat App here!
            } else {
                // Extracts the error message sent from the server
                String errorMsg = response.split(":")[2];
                statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                statusLabel.setText(errorMsg);
            }
        } catch (Exception e) {
            statusLabel.setText("Connection error.");
        }
    }
}
