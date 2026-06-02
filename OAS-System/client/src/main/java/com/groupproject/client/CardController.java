package com.groupproject.client;
import java.io.IOException;
import java.util.function.Consumer;

import com.groupproject.client.network.AuctionEventBus;
import com.groupproject.client.network.AuctionListener;
import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.CountDownHelper;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.client.utils.TimeUtil;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.events.AuctionCancelledEvent;
import com.groupproject.shared.network.events.AuctionEndedEvent;
import com.groupproject.shared.network.events.AuctionFinisedEvent;
import com.groupproject.shared.network.events.AuctionStartedEvent;
import com.groupproject.shared.network.requests.GetAuctionDetailRequest;
import com.groupproject.shared.network.requests.JoinAuctionRequest;
import com.groupproject.shared.network.responses.GetAuctionDetailResponse;
import com.groupproject.shared.network.responses.JoinAuctionResponse;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

public class CardController implements LifecycleController, AuctionListener { 
    private Auction auction;
    private Timeline timeline;

    private final Consumer<GetAuctionDetailResponse> getDetailListener = this::handleGetDetailAuction;
    private final Consumer<JoinAuctionResponse> joinAuctionListener = this::handleJoinAuctionResponse;

    @FXML private Label productname;
    @FXML private Label currentprice;
    @FXML private Label timeleft;

    @FXML private Label auctionStatus;
    @FXML private ToggleButton subscribeToggle;
    @FXML private Button cancelButton;

    @FXML
    public  void initialize() {

        // ĐĂNG KÝ VIỆC LẮNG NGHE TRẢ VỀ KẾT QUẢ
        ClientMessageRouter.INSTANCE.onResponse(JoinAuctionResponse.class,this::handleJoinAuctionResponse);
        ClientMessageRouter.INSTANCE.onResponse(GetAuctionDetailResponse.class, getDetailListener);
        ClientMessageRouter.INSTANCE.onResponse(JoinAuctionResponse.class, joinAuctionListener);
    }

    // Gọi hàm này từ HomeController để thêm dữ liệu vào card
    // ------------------------------------------------------
    public void setAuction(Auction auction) {
        this.auction = auction;

        // Thêm dữ liệu vào các ô
        productname.setText(auction.getTitle());

        // Hiện bid hiện tại nếu có người bid, không thì hiện giá khởi điểm
        double displayPrice = (auction.getCurrentBid() > 0) ? auction.getCurrentBid() : auction.getStartingPrice();
        currentprice.setText("$" + displayPrice);

        // Setup timer
        if (auction.getEndTime() != null) {
            timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
                updateCountDown();
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            updateCountDown(); // Run once immediately
        } else {
            timeleft.setText("No End Time");
        }
    }

    public void updateCountDown() {
        if (auction == null) return;
        if (auction.getEndTime() == null) return;

        String timeString = TimeUtil.formatTimeRemaining(auction.getEndTime());
        timeleft.setText(timeString);
    
        if ("ENDED".equals(timeString)) {
            if (timeline != null) timeline.stop();
        }
    }

    @FXML 
    private void handleBid(ActionEvent event) throws IOException {
        JoinAuctionRequest request = new JoinAuctionRequest(auction.getId());
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
   
    public void applyAuctionStatus(AuctionStatus status) {
        auctionStatus.setText("State : " + status );
    }
    
    @Override
    public void onAuctionStarted(AuctionStartedEvent event) {
        Platform.runLater(() -> {
            applyAuctionStatus(AuctionStatus.ACTIVATED);
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

    @Override
    public void cleanup() {
        ClientMessageRouter.INSTANCE.offResponse(GetAuctionDetailResponse.class, getDetailListener);
        ClientMessageRouter.INSTANCE.offResponse(JoinAuctionResponse.class, joinAuctionListener);
        if (timeline != null) timeline.stop();
        if (auction != null) {
            AuctionEventBus.getInstance().unsubscribe(auction.getId(), this);
        }
    }
}
