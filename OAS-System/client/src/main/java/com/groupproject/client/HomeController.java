package com.groupproject.client;
// giao dien, logic cua trang chu

import java.io.IOException;

import com.groupproject.client.network.RequestSender;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.GetAuctionRequest;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

// phan center cua mainscreen.fxml 
public class HomeController extends BaseAuctionViewController {
   
   @Override
   protected int getMaxColumns() {
       return 2;
   }

   @Override
   public boolean shouldInclude(Auction newItem) {
      return newItem.getStatus() == com.groupproject.shared.model.enums.AuctionStatus.ACTIVED;
   }

   @Override
   public void fetchInitialData() {
      GetAuctionRequest request = GetAuctionRequest.getByStatus(com.groupproject.shared.model.enums.AuctionStatus.ACTIVED, null);
      RequestSender.send(request);
   }

   @Override
   public void fetchDataByCategory(int categoryId) {
      GetAuctionRequest request = GetAuctionRequest.getByStatus(com.groupproject.shared.model.enums.AuctionStatus.ACTIVED, categoryId);
      RequestSender.send(request);
   }

   @Override
   public Node createCardNode(Auction auction) {
       try {
           FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/groupproject/client/FXML/testCard.fxml"));
           Node node = loader.load();
           TestCardController controller = loader.getController();
           controller.setAuction(auction);
           return node;
       } catch (IOException e) {
           e.printStackTrace();
           return null;
       }
   }
}
