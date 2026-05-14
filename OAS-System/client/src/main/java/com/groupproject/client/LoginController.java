package com.groupproject.client;

import java.io.IOException;

import com.groupproject.client.network.EventRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.network.LoginRequest;
import com.groupproject.shared.network.LoginResponse;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.paint.Color;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private ToggleButton eyeButton;
    @FXML private Button loginButton;

    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        // Vẫn giữ dòng này để đồng bộ chữ giữa 2 ô nhập mật khẩu
        passwordField.textProperty().bindBidirectional(passwordTextField.textProperty());

        EventRouter.getInstance().on(LoginResponse.class, this::handleLoginResponse);
        // mở lại nút login dù có thành công hay thất bại 
        loginButton.setDisable(false);
    }

    // Hàm này được gọi khi bấm nút con mắt (Do đã set onAction="#togglePasswordVisibility" trong FXML)
    @FXML
    private void togglePasswordVisibility(ActionEvent event) {
         boolean isVisible = eyeButton.isSelected();
        // Bật/tắt trạng thái hiển thị
        passwordField.setManaged(!isVisible);
        passwordField.setVisible(!isVisible);

        passwordTextField.setManaged(isVisible);
        passwordTextField.setVisible(isVisible);
        
    }
    
    // NOTE: You must pass your connected Socket/Streams to this controller 
    // before making these calls, just like your Chat controller!

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Please enter both fields.");
            return;
        }

        // Give the user some visual feedback while they wait
        // Vô hiệu hóa nút login tránh bấm quá nhiều lần
        statusLabel.setTextFill(Color.BLUE);
        statusLabel.setText("Logging in...");
        loginButton.setDisable(true);

        try {
            // Tạo và gửi yêu cầu đăng nhập cho server 
            LoginRequest loginRequest = new LoginRequest(username, password);

            RequestSender.send(loginRequest);

        } catch (Exception e) {
            statusLabel.setTextFill(Color.RED);
            statusLabel.setText("Connection error." + e.getMessage());
        }
    }

    @FXML 
    private void  switchtohome(ActionEvent event) throws IOException {
        SceneNavigator.getInstance().goTo("/com/groupproject/client/FXML/mainscreen.fxml");
    }
    @FXML
    private void switchtoSignup(ActionEvent event) throws IOException {
        SceneNavigator.getInstance().goTo("/com/groupproject/client/FXML/signup.fxml");
        //App.setRoot("signup");
    }

    // Hàm xử lý kết quả nhận về từ server
    private void handleLoginResponse(LoginResponse response) {
        System.out.println("LOGINCONTROLLLER:RESPONSE: Got response from server.");
        if (response.isSuccess()) { handleSuccessfulLogin(response); }
        else { handleFailedLogin(response); }
    }

    private void handleSuccessfulLogin(LoginResponse response) {
        // Thông báo cho client rằng đã thành công
        statusLabel.setTextFill(javafx.scene.paint.Color.GREEN);
        statusLabel.setText("Success! Loading chat...");
        // Lưu user
        SessionManager.getInstance().setCurrentUser(response.getUser());

        // TODO: Chuyển sang màn hình chính
        SceneNavigator.getInstance().goTo("/com/groupproject/client/FXML/mainscreen.fxml");
    }

    private void handleFailedLogin(LoginResponse response) {
        // Show error message on the screen
        System.out.println("Error: ");
        // errorLabel.setText(response.getMessage());
        statusLabel.setTextFill(Color.RED);
        statusLabel.setText(response.getMessage());
    }

}
