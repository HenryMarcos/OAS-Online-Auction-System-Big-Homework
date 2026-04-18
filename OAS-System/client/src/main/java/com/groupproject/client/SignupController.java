package com.groupproject.client;

import java.io.IOException;

import com.groupproject.shared.AuthRequest;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignupController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField repeatPasswordField;
    @FXML private Label statusLabel;

    @FXML
    private void handleSignUp() {
        // Lấy string từ file fxml
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String repeatPassword = repeatPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || 
            password.isEmpty() || repeatPassword.isEmpty()) {
            statusLabel.setText("Please enter all fields!");
            return;
        }

        if (!password.equals(repeatPassword)) {
            statusLabel.setText("Password do not match!: " + password + " | " + repeatPassword);
            return;
        }

        try {
            // Gửi chuỗi yêu cầu đăng ký 
            AuthRequest request = new AuthRequest("SIGNUP", username, email, password);
            App.out.writeObject(request);
            App.out.flush();

            // Chờ thông báo của server
            String response = (String) App.in.readObject();

            if (response.equals("SERVER:AUTH_SUCCESS")) {
                statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
                statusLabel.setText("Success! Loading chat...");
                App.setRoot("simpleChatApp"); // Transition to Chat
            } else {
                String errorMsg = response.split(":")[2];
                statusLabel.setTextFill(javafx.scene.paint.Color.RED);
                statusLabel.setText(errorMsg);
            }


        } catch (Exception e) {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Connection error.");
        }
    }

    @FXML
    private void switchToLogin() throws IOException {
        App.setRoot("login");
    }
}
