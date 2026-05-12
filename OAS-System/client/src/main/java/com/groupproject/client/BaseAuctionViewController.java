package com.groupproject.client;

import java.io.IOException;
import java.util.List;

import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.transaction.Auction;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public abstract class BaseAuctionViewController {
    @FXML
    protected  GridPane productgrid;
    @FXML
    protected  Button sortbutton;
    @FXML protected  HBox categoryBar;
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
   // hàm load những items có trong từng mục category

    // XỬ LÝ PHẦN PHẢN HỒI CỦA GETAUCTIONRESPONSE
    abstract  void handleAuctionsResponse();
    // GỬI LỜI NHẮC LÊN SEVER
    abstract void requestData();
    // HÀM TẢI AUCTIONS VỀ NẾU THÀNH CÔNG 
    abstract void loadAuctions();
    // 
}
