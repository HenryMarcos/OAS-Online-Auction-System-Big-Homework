package com.groupproject.client;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.requests.GetAuctionRequest;

public class MyAuctionsController extends BaseAuctionViewController {
   // hàm load những items có trong từng mục category
   @Override
   public boolean shouldInclude(Auction newItem) {
      return newItem.getSellerId()==SessionManager.INSTANCE.getCurrentUser().getId().intValue();
   }
   @Override
   public void fetchInitialData() {
      User user = SessionManager.INSTANCE.getCurrentUser();
      int idUser = user.getId().intValue();
      GetAuctionRequest request = GetAuctionRequest.getBySeller(idUser, null);
      RequestSender.send(request);
   }
   @Override 
   public void fetchDataByCategory(int categoryId) {
      User user = SessionManager.INSTANCE.getCurrentUser();
      int idUser = user.getId().intValue();
      GetAuctionRequest request = GetAuctionRequest.getBySeller(idUser, categoryId);
      RequestSender.send(request);
   }
    
}
