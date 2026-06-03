package com.groupproject.client;
// giao dien, logic cua trang chu

import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.GetAuctionRequest;

// phan center cua mainscreen.fxml 
public class HomeController extends BaseAuctionViewController {
    // hàm load những items có trong từng mục category
    @Override
    public boolean shouldInclude(Auction newItem) {
        return newItem.getStatus() == com.groupproject.shared.model.enums.AuctionStatus.ACTIVATED;
    }

    @Override
    public void fetchInitialData() {
        // 1. INSTANT LOAD: Immediately display whatever data we already have in memory!
        refreshFromSession();

        // 2. BACKGROUND REFRESH: Ask the server if there are any new updates.
        // (When the server replies, MainController will catch it and automatically redraw the screen again)
        GetAuctionRequest request = GetAuctionRequest.getByStatus(AuctionStatus.ACTIVATED, null);
        RequestSender.send(request);
    }

    @Override
    public void fetchDataByCategory(int categoryId) {
        GetAuctionRequest request = GetAuctionRequest.getByStatus(AuctionStatus.ACTIVATED, categoryId);
        RequestSender.send(request);
    }
    // Safely pulls from session, preventing infinite network loops!
    @Override
    public void refreshFromSession() {
        setAuctionsData(SessionManager.INSTANCE.getCurrentAuctionList());
    }
}
