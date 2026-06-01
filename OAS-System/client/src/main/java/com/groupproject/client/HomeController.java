package com.groupproject.client;
// giao dien, logic cua trang chu

import com.groupproject.client.network.RequestSender;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.GetAuctionRequest;




// phan center cua mainscreen.fxml 
public class HomeController extends BaseAuctionViewController  {
   // hàm load những items có trong từng mục category
   @Override
   public boolean shouldInclude(Auction newItem) {
      return true;
   }
   @Override
   public void fetchInitialData() {
      GetAuctionRequest request = GetAuctionRequest.getAll();
      RequestSender.send(request);
   }
   // TODO : PHẦN NÀY SẼ XỬ LÝ SAU 
   @Override
   public void fetchDataByCategory(int categoryId) {
      GetAuctionRequest request = GetAuctionRequest.getByStatus(null, categoryId);
      RequestSender.send(request);
   }
   
}
