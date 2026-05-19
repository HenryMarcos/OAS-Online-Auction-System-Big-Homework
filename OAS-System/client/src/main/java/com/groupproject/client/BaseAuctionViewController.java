package com.groupproject.client;
import java.io.IOException;
import java.util.List;

import com.groupproject.client.network.EventRouter;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.AuctionItem;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.network.CreateAuctionResponse;
import com.groupproject.shared.network.AuctionEvent.AuctionListener;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public abstract class BaseAuctionViewController implements AuctionListener {
    @FXML
    protected  GridPane productgrid;
    @FXML
    protected  Button sortbutton;
    @FXML protected  HBox categoryBar;
   protected ObservableList<AuctionItem> uiList = FXCollections.observableArrayList();
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
   public void drawCategoryUI() {
      categoryBar.getChildren().clear();
      List<Category> savedCategories = SessionManager.getInstance().getCurrentCategories();
      Button buttonAll= new Button("All");
      buttonAll.getStyleClass().add("category-btn");

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
   public void setupReactiveUI() {
      uiList.addListener((ListChangeListener<AuctionItem>) change -> {
         while (change.next()) {
            if (change.wasAdded()) {
               for (AuctionItem item : change.getAddedSubList()) {
                  Node cardNode= createCardNode(item);
                  Platform.runLater(() -> productgrid.getChildren().add(0,cardNode));
               }
            }
         }
      });
   }
   public Node createCardNode(AuctionItem item) {
      try {
         FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/groupproject/client/FXML/card.fxml"));
         Node node = loader.load();
         CardController cardController = loader.getController();
         cardController.populateUI(item);
         return node;
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }  
   }
   public void setupGlobalEventListeners() {
      EventRouter.getInstance().on(CreateAuctionResponse.class, response -> {
            if (response.isSuccess()) {
               AuctionItem newItem= response.getAuctionItem();
               if (shouldInclude(newItem)) {
                  Platform.runLater(() ->uiList.add(0,newItem));
               }
            }
      });
   }
   abstract  boolean shouldInclude(AuctionItem item);
   abstract void fetchInitialData();

}
