package com.groupproject.client;
// giao dien, logic cua trang chu
import java.io.IOException;
import java.net.URL;
import java.util.List;

import java.util.ResourceBundle;

import com.groupproject.shared.network.GetCategoriesRequest;
import com.groupproject.shared.network.GetCategoriesResponse;
import com.groupproject.client.Data.ItemRespository;
import com.groupproject.client.network.EventRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.user.User;
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
   @FXML private HBox categoryBar;
   // Khi nhan vao nut Log out o mep ben phai cua man hinh 

   // man hinh moi khi an vao nut sortby
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
   // hàm load những items có trong từng mục category
   public void loadItems() {
      /* 
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
         */
   }
   public static HomeController getInstance() {
         return instance;
   }

   // tao grid san pham dua vao cho gridproducts(flowpane)
   private void drawCategoryUI() {
      categoryBar.getChildren().clear();
      List<Category> savedCategories = SessionManager.getInstance().getCurrentCategories();
      Button buttonAll= new Button("All");
      buttonAll.getStyleClass().add("category-btn");
      buttonAll.setOnAction(e ->loadItems());

      for (Category category : savedCategories) {
               Button button = new Button(category.getName());
               button.getStyleClass().add("category-btn");
               button.setOnAction(e -> {
                  // test thu in ra console xem co hoat dong dung khong ? 
                  // gui yeu cau dua lay category id cua all, electronics,...
                  System.out.println("Dang loc items cua " + category.getName());
                  // gửi yêu cầu cho sever trả về các item theo trường thông tin này 
               });
               categoryBar.getChildren().add(button);
            }


   }
   
   
}
