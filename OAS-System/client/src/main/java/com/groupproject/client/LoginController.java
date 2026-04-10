package com.groupproject.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class LoginController {
    @FXML
    private Label login;
    @FXML
    private Label signuptext;

    @FXML
    protected void onHelloButtonClick() {
        login.setText("Hello");
    }
    @FXML
    protected void onSignupButtonClick() {
        signuptext.setText("Sign up");
    }
}
