package com.groupproject.client;
import java.io.IOException;

import com.groupproject.client.network.AuctionEventBus;
import com.groupproject.client.network.EventRouter;

import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.client.utils.CountDownHelper;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.AuctionEvent.AuctionCancelledEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionEndedEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionFinisedEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionListener;
import com.groupproject.shared.network.AuctionEvent.AuctionStartedEvent;
import com.groupproject.shared.network.AuctionEvent.BidUpdatedEvent;
import com.groupproject.shared.network.GetAuctionDetailRequest;
import com.groupproject.shared.network.GetAuctionDetailResponse;

import javafx.application.Platform;


public class CardController implements AuctionListener { 
    private Auction currentAuction;
    private CountDownHelper countDownHelper = new CountDownHelper();
    @FXML
    private ImageView image;
    @FXML
    private Label productname;
    @FXML
    private Label currentprice;
    @FXML
    private Label timeleft;
    @FXML private ToggleButton buttonSubscribe;
    @FXML private Label auctionStatus;

    @FXML
    public  void initialize() {
        // ĐĂN KÝ VIỆC LẮNG NGHE TRẢ VỀ KẾT QUẢ
        EventRouter.getInstance().on(GetAuctionDetailResponse.class, this::handleGetDetailAuction);
    }
    @FXML 
    private void handleBid(ActionEvent event) throws IOException {
        // Chỉ lưu ID vào session, AuctionController sẽ tự fetch từ Server khi initialize
        GetAuctionDetailRequest request = new GetAuctionDetailRequest(currentAuction.getId());
        RequestSender.send(request);
        
    }
    public void populateUI(Auction auction) {
        Platform.runLater(() -> {
            this.currentAuction = auction;
            productname.setText(auction.getTitle());
            currentprice.setText(String.valueOf(auction.getCurrentBid()));
            applyAuctionStatus(auction.getStatus());

        });
        
    }
    private void handleGetDetailAuction(GetAuctionDetailResponse response) {
        if (response.isSuccess()) {
            Platform.runLater(() ->{
                SessionManager.getInstance().setCurrentAuctionDetail(response.getAuctionDetail());
                SceneNavigator.goTo("/com/groupproject/client/FXML/auctionscreen.fxml");
            });
        }
        else {
            Platform.runLater(() -> {
                AlertUtils.showError("Error !" , "Can't enter the auction now ");
            });
        }
    }
    public void applyAuctionStatus(AuctionStatus status) {
        auctionStatus.setText("State : " + status );
    }
    @Override 
    public void onBidUpdated(BidUpdatedEvent event) {
        Platform.runLater(() -> {
            currentprice.setText(String.valueOf(event.getBidAmount())+"USD");
        });
    }
    @Override
    public void onAuctionStarted(AuctionStartedEvent event) {
        Platform.runLater(() -> {
            applyAuctionStatus(AuctionStatus.ACTIVED);
        });
    }

    @Override
    public void onAuctionCancelled(AuctionCancelledEvent event) {
        Platform.runLater(() -> {
            applyAuctionStatus(AuctionStatus.CANCELLED);
        });
    }

    @Override
    public void onAuctionEnded(AuctionEndedEvent event) {
        Platform.runLater(() -> {
            applyAuctionStatus(AuctionStatus.ENDED);
        });
    }

    @Override
    public void onAuctionFinished(AuctionFinisedEvent event) {
        // Tương tự như Ended, có thể thêm logic hiển thị người chiến thắng
        Platform.runLater(() -> {
            applyAuctionStatus(AuctionStatus.FINISHED);
        });
    }
}
