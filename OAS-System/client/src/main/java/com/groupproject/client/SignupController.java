package com.groupproject.client;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;;
public class SignupController {
    @FXML
    private TextField username;
    @FXML
    private TextField email;
    @FXML 
    private PasswordField password1;
    @FXML
    private PasswordField password2;
    @FXML
    private Hyperlink hyperlinklogin;
    
    @FXML
    private void switchtologin(ActionEvent event) throws IOException {
        HBox root = new HBox();
        VBox loginPane = FXMLLoader.load(getClass().getResource("FXML/login.fxml"));
        StackPane leftPane= new StackPane();
        leftPane.setStyle("-fx-background-color: black;");
        leftPane.prefWidthProperty().bind(root.widthProperty().divide(2));
        loginPane.prefWidthProperty().bind(root.widthProperty().divide(2));
        // Thêm 2 vùng vào HBox
        root.getChildren().addAll(leftPane, loginPane);
        // --- 3. Thiết lập Scene ---
        Scene scene = new Scene(root, 1000, 700);

        scene.getStylesheets().add(getClass().getResource("CSS/login.css").toExternalForm());
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Login | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(scene);
        currentStage.show();
    }
    
}
