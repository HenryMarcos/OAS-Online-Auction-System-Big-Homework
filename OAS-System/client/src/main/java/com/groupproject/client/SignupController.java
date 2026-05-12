package com.groupproject.client;

import java.io.IOException;

import com.groupproject.client.network.EventRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.network.SignupRequest;
import com.groupproject.shared.network.SignupResponse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;




public class SignupController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField repeatPasswordField;
    @FXML private Label statusLabel;
    @FXML private Button signupButton;
    @FXML private Hyperlink hyperlinklogin;

    @FXML
    public void initialize() {
        EventRouter.getInstance().on(SignupResponse.class, this::handleSignupResponse);
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        // Lấy string từ file fxml
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String repeatPassword = repeatPasswordField.getText();
        signupButton.setDisable(true);
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

            SignupRequest request = new SignupRequest(username, email, password, repeatPassword);
            RequestSender.send(request);

        } catch (Exception e) {
            statusLabel.setTextFill(javafx.scene.paint.Color.RED);
            statusLabel.setText("Connection error.");
        }
    }
    
    @FXML
    private void switchtologin(ActionEvent event) throws IOException {
        SceneNavigator.goTo("/com/groupproject/client/FXML/login.fxml");
    }

    // Hàm xử lý kết quả nhận về từ server
    private void handleSignupResponse(SignupResponse response) {
        if (response.isSuccess()) { handleSuccessfulSignup(response); }
        else { handleFailedSignup(response); }
        signupButton.setDisable(false);
    }

    private void handleSuccessfulSignup(SignupResponse response) {
        // Thông báo cho client rằng đã thành công
        statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
        statusLabel.setText("Success! Loading chat...");
        // Lưu user
        SessionManager.getInstance().setCurrentUser(response.getUser());
        SessionManager.getInstance().setCurrentCategories(response.getCategoryTree());
        
        System.out.println("Signup Success! Switching screens...");
        // chuyển sang màn hình chính
        SceneNavigator.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
    }

    private void handleFailedSignup(SignupResponse response) {
        // Show error message on the screen
        System.out.println("Error: ");
        // errorLabel.setText(response.getMessage());
        statusLabel.setTextFill(Color.RED);
        statusLabel.setText(response.getMessage());
    }
}

