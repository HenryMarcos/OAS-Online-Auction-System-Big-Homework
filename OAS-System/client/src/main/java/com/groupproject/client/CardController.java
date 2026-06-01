package com.groupproject.client;
import java.io.IOException;

import com.groupproject.client.network.AuctionEventBus;
import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.CountDownHelper;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.AuctionEvent.AuctionCancelledEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionFinisedEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionListener;
import com.groupproject.shared.network.AuctionEvent.AuctionStartedEvent;
import com.groupproject.shared.network.AuctionEvent.BidUpdatedEvent;
import com.groupproject.shared.network.events.AuctionEndedEvent;
import com.groupproject.shared.network.requests.GetAuctionDetailRequest;
import com.groupproject.shared.network.requests.JoinAuctionRequest;
import com.groupproject.shared.network.responses.GetAuctionDetailResponse;
import com.groupproject.shared.network.responses.JoinAuctionResponse;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

public class CardController implements AuctionListener { 
    private Auction auction;
    private Timeline timeline;

    @FXML private Label productname;
    @FXML private Label currentprice;
    @FXML private Label timeleft;

    @FXML private Label auctionStatus;
    @FXML private ToggleButton subscribeToggle;
    @FXML private Button cancelButton;

    @FXML
    public  void initialize() {
        populateUI(auction);

        // ĐĂNG KÝ VIỆC LẮNG NGHE TRẢ VỀ KẾT QUẢ
        ClientMessageRouter.INSTANCE.onResponse(GetAuctionDetailResponse.class, this::handleGetDetailAuction);
        ClientMessageRouter.INSTANCE.onResponse(JoinAuctionResponse.class,this::handleJoinAuctionResponse);
    }
    @FXML 
    private void handleBid(ActionEvent event) throws IOException {
        // Chỉ lưu ID vào session, AuctionController sẽ tự fetch từ Server khi initialize
        GetAuctionDetailRequest request = new GetAuctionDetailRequest(auction.getId());
        RequestSender.send(request);
    }
    public void populateUI(Auction auction) {
        Platform.runLater(() -> {
            this.auction = auction;
            productname.setText(auction.getTitle());
            currentprice.setText(String.valueOf(auction.getCurrentBid()));
            CountDownHelper countDownHelper = new CountDownHelper();
            countDownHelper.start(auction, () -> timeleft.setText("ENDED"), timeleft);
            applyAuctionStatus(auction.getStatus());

            // NGHIỆP VỤ ĐỂ HIỂN THỊ NÚT HỦY PHIÊN 
            if (auction.getStatus()==AuctionStatus.WAITING) {
                cancelButton.setVisible(true);
                cancelButton.setManaged(true);
            }
        });
    }
    private void handleGetDetailAuction(GetAuctionDetailResponse response) {
        if (response.isSuccess()) {
            Platform.runLater(() ->{
                SessionManager.INSTANCE.setCurrentAuctionDetail(response.getAuctionDetail());
                SceneNavigator.INSTANCE.goTo("/com/groupproject/client/FXML/auctionscreen.fxml");
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
    @FXML
    private void handleSubscribeToggle(ActionEvent event) {
        if (subscribeToggle.isSelected()) {
            subscribeToggle.setText("UNFOLLOW");
            User user= SessionManager.INSTANCE.getCurrentUser();
            // GỬI THÔNG BÁO MUỐN NHẬN TIN CỦA PHIÊN ĐẤU GIÁ NÀY LÊN SERVER
            RequestSender.send(new JoinAuctionRequest(auction.getId()));
            // CLIENTLOGGER GHI LAI SU KIEN
        }
        else {
            subscribeToggle.setText("FOLLOW NOW !");
            AuctionEventBus.getInstance().unsubscribe(auction.getId(), this);
            // XU LY KHI HO HUY THONG BAO
        }
    }
    @FXML
    private void handleCancelAuction(ActionEvent event) {

    }
    private void handleJoinAuctionResponse(JoinAuctionResponse response) {
        if (response.isSuccess()) {
            AuctionEventBus.getInstance().subscribe(auction.getId(), this);
        }
        else {
            // Thong bao cho khac hang la ho da dang ky that bai 
        }
    }
}
