package com.groupproject.client;
import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;



public class Login extends Application {

    private PasswordField passwordField;
    private TextField passwordTextField;
    private ToggleButton eyeButton;

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("Login | Auction System ");
        Parent root = FXMLLoader.load(getClass().getResource("FXML/login.fxml"));
        Scene scene = new Scene(root,1000,700);
        scene.getStylesheets().add(getClass().getResource("CSS/login.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args)  {
        launch(args);
    }
}
