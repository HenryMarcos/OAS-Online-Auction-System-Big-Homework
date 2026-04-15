package com.groupproject.client;
// giao dien, logic cua trang chu 
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class HomeController {
   // Khi nhan vao nut Log out o mep ben phai cua man hinh 
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
   // Khi nhan vao nut Home o man hinh chinh 
   @FXML
   private void switchtoHome(ActionEvent event) throws IOException {
      Parent root = FXMLLoader.load(getClass().getResource("/com/groupproject/client/FXML/home.fxml"));
    
        // Bước 2: Tạo một Scene (Cảnh diễn) mới từ giao diện vừa tải
        Scene newScene = new Scene(root,1000,700);
        newScene.getStylesheets().add(getClass().getResource("CSS/home.css").toExternalForm());
        // Bước 3: Lấy lại Sân khấu (Stage) hiện tại từ nút bấm mà người dùng vừa click
        Stage currentStage =  (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Home | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(newScene);
        currentStage.show();
   } 
   @FXML
   private void switchtoAddItem(ActionEvent event) throws IOException {
      Parent root = FXMLLoader.load(getClass().getResource("/com/groupproject/client/FXML/additem.fxml"));
      Scene scene = new Scene(root,1000,700);
      scene.getStylesheets().add(getClass().getResource("CSS/additem.css").toExternalForm());
      Stage currentStage = new Stage();
      currentStage.setTitle("Add Item Screen | Auction System");
      currentStage.setScene(scene);
      currentStage.show();
   }
}
