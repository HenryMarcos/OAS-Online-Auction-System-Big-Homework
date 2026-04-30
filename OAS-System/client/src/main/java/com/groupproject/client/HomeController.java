package com.groupproject.client;
// giao dien, logic cua trang chu 
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class HomeController implements Initializable {
   @FXML
   private FlowPane productgrid;
   @FXML
   private Button sortbutton;
   // Khi nhan vao nut Log out o mep ben phai cua man hinh 
   @FXML 
   private void switchtologin(ActionEvent event) throws IOException {
        
         Parent root = FXMLLoader.load(getClass().getResource("FXML/login.fxml"));
        //StackPane leftPane= new StackPane();
        //leftPane.setStyle("-fx-background-color: black;");
        //leftPane.prefWidthProperty().bind(root.widthProperty().divide(2));
        //loginPane.prefWidthProperty().bind(root.widthProperty().divide(2));
        // Thêm 2 vùng vào HBox
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
      Scene scene = new Scene(root,1000,850);
      scene.getStylesheets().add(getClass().getResource("CSS/additem.css").toExternalForm());
      scene.getStylesheets().add(getClass().getResource("CSS/home.css").toExternalForm());
      Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      currentStage.setTitle("Add Item Screen | Auction System");
      currentStage.setScene(scene);
      currentStage.show();
   }
   @Override
   public void initialize(URL location, ResourceBundle resources) {
         loadProducts();
         addEventHandles();
   }
   private void loadProducts() {
      System.out.println("Them san pham vao");
   }
   public void addEventHandles()  {
      sortbutton.setOnMouseClicked(mouseEvent -> {
         Stage stage = new Stage();
         FXMLLoader loader = new FXMLLoader();
         loader.setLocation(App.class.getResource("/com/groupproject/client/FXML/sortmenu.fxml"));
         try {
               AnchorPane root = loader.load();
               stage.setScene(new Scene(root));
               stage.initStyle(StageStyle.TRANSPARENT);
               stage.show();
         } catch (IOException e) {
               e.printStackTrace();
         }
      });
   }
   // tao grid san pham dua vao cho gridproducts(flowpane)
   
   
   
}
