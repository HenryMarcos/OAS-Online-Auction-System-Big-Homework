package com.groupproject.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.groupproject.shared.AuthRequest;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    
    // NOTE: You must pass your connected Socket/Streams to this controller 
    // before making these calls, just like your Chat controller!

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both fields.");
            return;
        }

        try {
            // Tạo và gửi yêu cầu đăng nhập cho server
            AuthRequest request = new AuthRequest("LOGIN", username, password);
            App.out.writeObject(request);
            App.out.flush();

            // Wait for the server's response
            String response = (String) App.in.readObject();
            
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

    @FXML
    private void switchToSignup() throws IOException {
        App.setRoot("signup");
    }

    public void initialize() {
    
    }
}
