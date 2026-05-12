package com.groupproject.client;

import java.io.IOException;
import java.util.List;

import com.groupproject.client.network.EventRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.GetAuctionRequest;
import com.groupproject.shared.network.GetAuctionResponse;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class ActiveAuctionsController extends BaseAuctionViewController {
    @FXML
   public void initialize() {
      drawCategoryUI();
      addEventHandles();
      handleAuctionsResponse();
      // Nhận thông báo
      // Gửi thông báo 
      requestData();
   }
   // hàm load những items có trong từng mục category
   @Override
   public void loadAuctions() {
      productgrid.getChildren().clear();
      
      List<Auction> auctions = SessionManager.getInstance().getActiveAuctions();
      for (int i=0; i< auctions.size();i++) {
         try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/groupproject/client/FXML/card.fxml"));
            HBox card = (HBox) loader.load();
            card.setMaxWidth(Double.MAX_VALUE);
            GridPane.setFillWidth(card, true);
            CardController controller = loader.getController();
            controller.populateUI(auctions.get(i));
            productgrid.add(card,i % 2,i /2);

         }
         catch( IOException e) {
            e.printStackTrace();
         }
      }
   }
   @Override
   public void requestData() {
      GetAuctionRequest request = GetAuctionRequest.getActivedAuctions();
      RequestSender.send(request);
   }

   // tao grid san pham dua vao cho gridproducts(flowpane)
   @Override
   public void  handleAuctionsResponse() {
         EventRouter.getInstance().on(GetAuctionResponse.class, response -> {
            if (response.isSuccess()) {
               Platform.runLater(() -> {
                  SessionManager.getInstance().setActiveAuction(response.getAuction());
                  loadAuctions();
               });
            }
            else {
                Platform.runLater(() -> {
                     AlertUtils.showError("Error !", "Can't not load all auctions ");
                });
            }
         });
   }
   
   
}

