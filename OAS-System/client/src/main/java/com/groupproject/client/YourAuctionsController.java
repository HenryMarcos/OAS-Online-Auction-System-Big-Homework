package com.groupproject.client;

import java.io.IOException;
import java.util.List;

import com.groupproject.client.network.EventRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.AuctionItem;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.GetAuctionItemRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class YourAuctionsController extends BaseAuctionViewController {
    @FXML
   public void initialize() {
      drawCategoryUI();
      addEventHandles();
      setupGlobalEventListeners();
      fetchInitialData();
      setupReacticeUI();
   }
   // hàm load những items có trong từng mục category
   @Override
   public boolean shouldInclude(AuctionItem newItem) {
      return newItem.getSellerId()==;
   }
   @Override
   public void fetchInitialData() {
      GetAuctionItemRequest request = GetAuctionItemRequest.getMyAuctionItems();
      RequestSender.send(request);
   }
    
}
