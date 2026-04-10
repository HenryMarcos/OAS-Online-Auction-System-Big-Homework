package com.groupproject.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;   
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Login extends Application {

    private PasswordField passwordField;
    private TextField passwordTextField;
    private ToggleButton eyeButton;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Login | Auction System");
        // --- 1. Dùng HBox
        HBox root = new HBox();

        // Vùng trái (Màu đen)
        StackPane leftPane = new StackPane();
        leftPane.setStyle("-fx-background-color: black;");

        // Vùng phải (Form Đăng nhập)
        VBox loginPane = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML/login.fxml"));
            loginPane = loader.load();
        }
        catch (Exception e) {
            e.printStackTrace();

        }
        if (loginPane != null) {
            leftPane.prefWidthProperty().bind(root.widthProperty().divide(2));
            loginPane.prefWidthProperty().bind(root.widthProperty().divide(2));
            // Thêm 2 vùng vào HBox
            root.getChildren().addAll(leftPane, loginPane);
            // --- 3. Thiết lập Scene ---
            Scene scene = new Scene(root, 1000, 700);

            // Load file CSS (Dùng đường dẫn tương đối nếu file css để cùng thư mục với Main.java)
            scene.getStylesheets().add(getClass().getResource("CSS/login.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.show();
        }
        else {
            System.err.println("Loi can phai sua");
        }
        
    }
    /* 
    // Hàm xử lý logic hiện/ẩn mật khẩu
    private void togglePasswordVisibility() {
        boolean isVisible = eyeButton.isSelected();
        // Bật/tắt trạng thái hiển thị
        passwordField.setManaged(!isVisible);
        passwordField.setVisible(!isVisible);

        passwordTextField.setManaged(isVisible);
        passwordTextField.setVisible(isVisible);
    }
    */
    public static void main(String[] args) {
        launch(args);
    }
}
