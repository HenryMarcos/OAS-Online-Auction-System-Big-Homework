package com.groupproject.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

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
}
