package com.groupproject.client;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;


public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private ToggleButton eyeButton;

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
    }
}
