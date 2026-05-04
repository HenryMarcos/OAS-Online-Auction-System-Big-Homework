package com.groupproject.client;

import java.io.IOException;

import javafx.fxml.FXML;

public class PrimaryController {

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

    @FXML
    private void shootUserOverTheWire() throws IOException {
        App.shootUserOverTheWire();
    }

    @FXML
    private void switchToSimpleChatApp() throws IOException {
        App.setRoot("simpleChatApp");
    }

    @FXML
    private void switchToLogin() throws IOException {
        App.setRoot("login");
    }
}
