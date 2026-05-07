package com.groupproject.client;
// giao dien, logic cua trang chu
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.groupproject.client.Utlis.Item;
import com.groupproject.client.Utlis.ItemRespository;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


// phan center cua mainscreen.fxml 
public class HomeController implements  Initializable {
   private static HomeController instance;
   @FXML
   private GridPane productgrid;
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
      instance = this;
      addEventHandles();
      loadItems();
   }
   public void loadItems() {
      productgrid.getChildren().clear();
      
      List<Item> items = ItemRespository.getAll();
      for (int i=0; i< items.size();i++) {
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/groupproject/client/FXML/card.fxml"));
            HBox card = (HBox) loader.load();
            card.setMaxWidth(Double.MAX_VALUE);
            GridPane.setFillWidth(card, true);
            CardController controller = loader.getController();
            controller.setItem(items.get(i));
            productgrid.add(card,i % 2,i /2);

         }
         catch( IOException e) {
            e.printStackTrace();
         }
      }
   }
   public static HomeController getInstance() {
         return instance;
   }

   // tao grid san pham dua vao cho gridproducts(flowpane)
   
   
   
}
