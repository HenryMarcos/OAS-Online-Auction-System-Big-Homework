package com.groupproject.client;
import com.groupproject.client.network.RequestSender;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.network.GetAuctionRequest;

public class ActiveAuctionsController extends BaseAuctionViewController {
   // hàm load những items có trong từng mục category
   @Override
   public boolean shouldInclude(Auction newItem) {
      return newItem.getStatus()==AuctionStatus.ACTIVED;
   }
   @Override
   public void fetchInitialData() {
      GetAuctionRequest request = GetAuctionRequest.getByStatus(AuctionStatus.ACTIVED, null);
      RequestSender.send(request);
   }
   @Override
   public void fetchDataByCategory(int categoryId) {
      GetAuctionRequest request = GetAuctionRequest.getByStatus(AuctionStatus.ACTIVED, categoryId);
      RequestSender.send(request);
   }
   
}

