package com.groupproject.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
        VBox loginPane = createLoginPane();

        // Ép chiều rộng của vùng trái và vùng phải luôn bằng đúng 1/2 chiều rộng của HBox tổng
        leftPane.prefWidthProperty().bind(root.widthProperty().divide(2));
        loginPane.prefWidthProperty().bind(root.widthProperty().divide(2));

        // Thêm 2 vùng vào HBox
        root.getChildren().addAll(leftPane, loginPane);

        // --- 3. Thiết lập Scene ---
        Scene scene = new Scene(root, 1000, 700);

        // Load file CSS (Dùng đường dẫn tương đối nếu file css để cùng thư mục với Main.java)
        scene.getStylesheets().add(getClass().getResource("/com/groupproject/client/CSS/login.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createLoginPane() {
        VBox loginPane = new VBox();
        loginPane.getStyleClass().add("login-pane");
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setPadding(new Insets(50, 80, 50, 80));
        loginPane.setSpacing(20);

        // --- A. Header (Signup) ---
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_RIGHT);
        headerBox.setSpacing(10);

        Label dontHaveAccountLabel = new Label("Didn't have account ?");
        dontHaveAccountLabel.getStyleClass().add("plain-text");

        Button signupButton = new Button("Signup");
        signupButton.getStyleClass().add("signup-button");

        headerBox.getChildren().addAll(dontHaveAccountLabel, signupButton);
        loginPane.getChildren().add(headerBox);

        // --- B. Tiêu đề ---
        Label loginTitleLabel = new Label("Login");
        loginTitleLabel.getStyleClass().add("login-title");
        VBox.setMargin(loginTitleLabel, new Insets(80, 0, 40, 0));
        loginPane.getChildren().add(loginTitleLabel);

        // --- C. Username ---
        VBox usernameBox = new VBox(5);
        usernameBox.setMaxWidth(400);

        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("field-label");

        TextField usernameField = new TextField();
        usernameField.getStyleClass().add("input-field");

        usernameBox.getChildren().addAll(usernameLabel, usernameField);
        loginPane.getChildren().add(usernameBox);

        // --- D. Password (Có chức năng hiện/ẩn) ---
        VBox passwordBox = new VBox(5);
        passwordBox.setMaxWidth(400);

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("field-label");

        HBox passwordWrapper = new HBox();
        passwordWrapper.getStyleClass().add("password-wrapper");
        passwordWrapper.setAlignment(Pos.CENTER_LEFT);

        // Trường nhập mật khẩu (dấu chấm)
        passwordField = new PasswordField();
        passwordField.getStyleClass().add("inner-password-field");
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        // Trường hiển thị văn bản (khi bấm mở mắt)
        passwordTextField = new TextField();
        passwordTextField.getStyleClass().add("inner-password-field");
        passwordTextField.setManaged(false); // Ẩn mặc định
        passwordTextField.setVisible(false);
        HBox.setHgrow(passwordTextField, Priority.ALWAYS);

        // Đồng bộ dữ liệu giữa 2 trường
        passwordField.textProperty().bindBidirectional(passwordTextField.textProperty());

        // Nút con mắt
        eyeButton = new ToggleButton();
        eyeButton.getStyleClass().add("eye-button");
        eyeButton.setOnAction(e -> togglePasswordVisibility());

        passwordWrapper.getChildren().addAll(passwordField, passwordTextField, eyeButton);
        passwordBox.getChildren().addAll(passwordLabel, passwordWrapper);
        loginPane.getChildren().add(passwordBox);

        // --- E. Footer (Forgot password & Login Button) ---
        HBox footerBox = new HBox();
        footerBox.setAlignment(Pos.CENTER);
        footerBox.setSpacing(0); // Khoảng cách giữa quên mật khẩu và nút
        footerBox.setMaxWidth(400);
        VBox.setMargin(footerBox, new Insets(20, 0, 0, 0));
        Hyperlink forgotPasswordLink = new Hyperlink("Forgot Password?");
        forgotPasswordLink.getStyleClass().add("forgot-password-link");

        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("login-button");
        loginButton.setMinWidth(120);

        // Đẩy nút Login sang phải
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footerBox.getChildren().addAll(forgotPasswordLink, spacer, loginButton);
        loginPane.getChildren().add(footerBox);

        return loginPane;
    }

    // Hàm xử lý logic hiện/ẩn mật khẩu
    private void togglePasswordVisibility() {
        boolean isVisible = eyeButton.isSelected();
        // Bật/tắt trạng thái hiển thị
        passwordField.setManaged(!isVisible);
        passwordField.setVisible(!isVisible);

        passwordTextField.setManaged(isVisible);
        passwordTextField.setVisible(isVisible);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
