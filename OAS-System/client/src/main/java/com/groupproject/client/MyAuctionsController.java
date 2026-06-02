package com.groupproject.client;

import java.io.IOException;

import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.requests.GetAuctionRequest;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

public class MyAuctionsController extends BaseAuctionViewController {

    @Override
    protected int getMaxColumns() {
        return 1;
    }

    // Lọc để hiện tất cả auction của seller trong live updates
    @Override
    public boolean shouldInclude(Auction newItem) {
        User user = SessionManager.INSTANCE.getCurrentUser();
        boolean isMine = (user != null && newItem.getSellerId() == user.getId().intValue());
        return isMine && (newItem.getStatus() == com.groupproject.shared.model.enums.AuctionStatus.WAITING || 
                          newItem.getStatus() == com.groupproject.shared.model.enums.AuctionStatus.SCHEDULED || 
                          newItem.getStatus() == com.groupproject.shared.model.enums.AuctionStatus.ACTIVED);
    }

    @Override
    public void fetchInitialData() {
        User user = SessionManager.INSTANCE.getCurrentUser();
        int idUser = user.getId().intValue();
        // status = null → lấy tất cả trạng thái (WAITING, SCHEDULED, ACTIVED, ENDED, CANCELLED, FINISHED)
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

    /**
     * Override để dùng myAuctionCard.fxml riêng thay vì card.fxml chung.
     * Card này có nút "Bắt đầu ngay" và "Hủy phiên".
     */
    @Override
    public Node createCardNode(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/groupproject/client/FXML/myAuctionCard.fxml")
            );
            Node node = loader.load();
            MyAuctionCardController ctrl = loader.getController();
            ctrl.setAuction(auction);
            return node;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
