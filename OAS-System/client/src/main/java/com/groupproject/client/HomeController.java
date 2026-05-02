package com.groupproject.client;
// giao dien, logic cua trang chu 
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class HomeController implements  Initializable {
   @FXML
   private FlowPane productgrid;
   @FXML
   private Button sortbutton;
   // Khi nhan vao nut Log out o mep ben phai cua man hinh 
  
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
   @Override
   public void initialize(URL location, ResourceBundle resources) {
      addEventHandles();
   }
   

   // tao grid san pham dua vao cho gridproducts(flowpane)
   
   
   
}
