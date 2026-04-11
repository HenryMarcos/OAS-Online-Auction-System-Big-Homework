package com.groupproject.client;
// giao dien, logic cua trang chu 
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Stack;


public class HomeController {
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

        // Load file CSS (Dùng đường dẫn tương đối nếu file css để cùng thư mục với Main.java)
        scene.getStylesheets().add(getClass().getResource("CSS/login.css").toExternalForm());
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Login | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(scene);
        currentStage.show();

   }
}
