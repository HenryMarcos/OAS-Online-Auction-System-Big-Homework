package com.groupproject.client; // Note: Change to com.groupproject.client.controller.home if you applied the folder reorganization!

import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.GetAuctionRequest;

public class ActiveAuctionsController extends BaseAuctionViewController {

   @Override
   public boolean shouldInclude(Auction newItem) {
      // The parent class will use this to automatically filter out anything that isn't ACTIVATED
      return newItem != null && newItem.getStatus() == AuctionStatus.ACTIVATED;
   }

   @Override
   public void fetchInitialData() {
      GetAuctionRequest request = GetAuctionRequest.getByStatus(AuctionStatus.ACTIVATED, null);
      RequestSender.send(request);
   }

   @Override
   public void fetchDataByCategory(int categoryId) {
      GetAuctionRequest request = GetAuctionRequest.getByStatus(AuctionStatus.ACTIVATED, categoryId);
      RequestSender.send(request);
   }

   // 🌟 FIX: Added the missing method to prevent infinite server loops!
   @Override
   public void refreshFromSession() {
      // Grab the master list from memory. 
      // The Parent class will automatically pass it through your shouldInclude() filter above!
      setAuctionsData(SessionManager.INSTANCE.getCurrentAuctionList());
   }
}