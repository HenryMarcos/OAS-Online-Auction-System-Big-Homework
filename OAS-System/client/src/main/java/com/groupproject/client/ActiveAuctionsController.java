package com.groupproject.client;
import com.groupproject.client.network.RequestSender;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.network.GetAuctionRequest;
import javafx.fxml.FXML;
public class ActiveAuctionsController extends BaseAuctionViewController {
    @FXML
   public void initialize() {
      drawCategoryUI();
      addEventHandles();
      // Nhận thông báo
      // Gửi thông báo 
      setupGlobalEventListeners();
      fetchInitialData();
      setupReactiveUI();
   }
   // hàm load những items có trong từng mục category
   @Override
   public boolean shouldInclude(Auction newItem) {
      return newItem.getStatus()==AuctionStatus.ACTIVED;
   }
   @Override
   public void fetchInitialData() {
      GetAuctionRequest request = GetAuctionRequest.getActivedAuctions();
      RequestSender.send(request);
   }
   
   
}

