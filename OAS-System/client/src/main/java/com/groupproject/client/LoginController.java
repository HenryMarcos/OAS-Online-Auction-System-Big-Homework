package com.groupproject.client;

import java.io.IOException;

import com.groupproject.shared.AuthRequest;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private ToggleButton eyeButton;


    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        // Vẫn giữ dòng này để đồng bộ chữ giữa 2 ô nhập mật khẩu
        passwordField.textProperty().bindBidirectional(passwordTextField.textProperty());
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
    private void  switchtohome(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/groupproject/client/FXML/mainscreen.fxml"));
    
        // Bước 2: Tạo một Scene (Cảnh diễn) mới từ giao diện vừa tải
        Scene newScene = new Scene(root,1000,700);
        // Bước 3: Lấy lại Sân khấu (Stage) hiện tại từ nút bấm mà người dùng vừa click
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Home | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(newScene);
        currentStage.show();
    }
    @FXML
    private void switchtoSignup(ActionEvent event) throws IOException {
        Parent root= FXMLLoader.load(getClass().getResource("/com/groupproject/client/FXML/signup.fxml"));
        Scene newScene= new Scene(root,1000,700);
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Sign up | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(newScene);
        currentStage.show();

        //App.setRoot("signup");
    }

}
