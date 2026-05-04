package com.groupproject.client;

import java.io.IOException;

import com.groupproject.shared.AuthRequest;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;



public class SignupController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField repeatPasswordField;
    @FXML private Label statusLabel;

    @FXML private Hyperlink hyperlinklogin;

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
    private void switchtologin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("FXML/login.fxml"));
        Scene scene = new Scene(root,1000,700);
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Login | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(scene);
        currentStage.show();
        //App.setRoot("login");
    }
}

