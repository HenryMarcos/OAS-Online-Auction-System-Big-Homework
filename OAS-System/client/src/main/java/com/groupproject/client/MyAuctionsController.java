package com.groupproject.client;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.requests.GetAuctionRequest;

public class MyAuctionsController extends BaseAuctionViewController {
    @Override
    public boolean shouldInclude(Auction newItem) {
        User user = SessionManager.INSTANCE.getCurrentUser();
        return user != null && newItem.getSellerId() == user.getId().intValue();
    }

    @Override
    public void fetchInitialData() {
        User user = SessionManager.INSTANCE.getCurrentUser();
        if (user != null) {
            RequestSender.send(GetAuctionRequest.getBySeller(user.getId().intValue(), null));
        }
    }

    @Override
    public void fetchDataByCategory(int categoryId) {
        User user = SessionManager.INSTANCE.getCurrentUser();
        if (user != null) {
            RequestSender.send(GetAuctionRequest.getBySeller(user.getId().intValue(), categoryId));
        }
    }

    @Override
    public void refreshFromSession() {
        setAuctionsData(SessionManager.INSTANCE.getMyProductList());
    }
}
