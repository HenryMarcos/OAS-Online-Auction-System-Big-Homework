package com.groupproject.client;
import com.groupproject.shared.model.user.User;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.GetAuctionRequest;
import javafx.fxml.FXML;
public class YourAuctionsController extends BaseAuctionViewController {
    @FXML
   public void initialize() {
      drawCategoryUI();
      addEventHandles();
      setupGlobalEventListeners();
      fetchInitialData();
      setupReactiveUI();
   }
   // hàm load những items có trong từng mục category
   @Override
   public boolean shouldInclude(Auction newItem) {
      return newItem.getSellerId()==SessionManager.getInstance().getCurrentUser().getId().intValue();
   }
   @Override
   public void fetchInitialData() {
      User user = SessionManager.getInstance().getCurrentUser();
      int idUser = user.getId().intValue();
      GetAuctionRequest request = GetAuctionRequest.getMyAuctions(idUser);
      RequestSender.send(request);
   }
    
}
