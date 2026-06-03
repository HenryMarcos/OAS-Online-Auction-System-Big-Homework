package com.groupproject.client;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.stage.Stage; 
import java.io.IOException;
public class SortingmenuController {
    @FXML
    private ImageView closeButton;
    @FXML
    private void closeSortmenu() throws IOException {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
