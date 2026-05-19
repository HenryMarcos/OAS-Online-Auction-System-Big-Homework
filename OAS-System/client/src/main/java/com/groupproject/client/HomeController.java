package com.groupproject.client;
// giao dien, logic cua trang chu

import com.groupproject.client.network.RequestSender;

import com.groupproject.shared.model.transaction.AuctionItem;


import com.groupproject.shared.network.GetAuctionItemRequest;

import javafx.fxml.FXML;




// phan center cua mainscreen.fxml 
public class HomeController extends BaseAuctionViewController  {
   @FXML
   public void initialize() {
      // ĐƯỢC KẾ THỪA TỪ ABSTRACT CLASS
      drawCategoryUI();
      addEventHandles();
      // ĐƯỢC OVERRIDE NGAY TẠI HÀM CON
      setupGlobalEventListeners();
      fetchInitialData();
      setupReactiveUI();
   }
   // hàm load những items có trong từng mục category
   @Override
   public boolean shouldInclude(AuctionItem newItem) {
      return true;
   }
   @Override
   public void fetchInitialData() {
      GetAuctionItemRequest request = GetAuctionItemRequest.getAllAuctionItems();
      RequestSender.send(request);
   }
}
