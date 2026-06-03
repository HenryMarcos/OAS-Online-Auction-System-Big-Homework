package com.groupproject.client;

import com.groupproject.client.network.AuctionEventBus;
import com.groupproject.client.network.AuctionListener;
import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.CountDownHelper;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.events.NewBidEvent;
import com.groupproject.shared.network.requests.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.requests.JoinAuctionRequest;
import com.groupproject.shared.network.responses.ChangeAuctionStatusResponse;
import com.groupproject.shared.network.responses.GetAuctionDetailResponse;
import com.groupproject.shared.network.responses.JoinAuctionResponse;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;

public class CardController implements LifecycleController, AuctionListener {

    private Auction auction;
    private CountDownHelper countDownHelper = new CountDownHelper();

    // Event Listeners
    private final java.util.function.Consumer<JoinAuctionResponse> joinAuctionListener = this::handleJoinAuctionResponse;
    private final java.util.function.Consumer<GetAuctionDetailResponse> getDetailListener = this::handleGetDetailAuction;
    private final java.util.function.Consumer<ChangeAuctionStatusResponse> statusChangeListener = this::handleStatusChangeResponse;

    @FXML private ImageView image;
    @FXML private Label productname;
    @FXML private Label auctionStatus;
    @FXML private Label currentprice;
    @FXML private Label timeleft;
    
    @FXML private ToggleButton subcribeToggle;
    @FXML private Button startNowButton;
    @FXML private Button cancelAuctionButton;
    @FXML private Button viewAuctionButton;

    public void setAuction(Auction auction) {
        this.auction = auction;

        // Register Global Handlers
        ClientMessageRouter.INSTANCE.onResponse(JoinAuctionResponse.class, joinAuctionListener);
        ClientMessageRouter.INSTANCE.onResponse(GetAuctionDetailResponse.class, getDetailListener);
        ClientMessageRouter.INSTANCE.onResponse(ChangeAuctionStatusResponse.class, statusChangeListener);
        
        // Listen to Live Updates
        AuctionEventBus.getInstance().subscribe(auction.getId(), this);

        updateUI();
    }

    private void updateUI() {

        // --- ADD THIS IMAGE LOADING LOGIC ---
        if (auction.getMainImageBytes() != null && auction.getMainImageBytes().length > 0) {
            try {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(auction.getMainImageBytes());
                javafx.scene.image.Image img = new javafx.scene.image.Image(bis);
                image.setImage(img);
            } catch (Exception e) {
                System.err.println("Could not load image for card: " + auction.getId());
            }
        } else {
            // Keep the default sample image if no image was provided
        }

        productname.setText(auction.getTitle());
        
        double displayPrice = (auction.getCurrentBid() > 0) ? auction.getCurrentBid() : auction.getStartingPrice();
        currentprice.setText(String.format("$%.2f", displayPrice));
        auctionStatus.setText(statusLabel(auction.getStatus()));

        countDownHelper.start(auction, () -> Platform.runLater(this::updateUI), timeleft);

        // --- THE MAGIC: UI ADAPTATION LOGIC ---
        User currentUser = SessionManager.INSTANCE.getCurrentUser();
        boolean isSeller = (currentUser != null && currentUser.getId().intValue() == auction.getSellerId());

        if (isSeller) {
            // It's MY auction
            subcribeToggle.setVisible(false);
            subcribeToggle.setManaged(false);

            boolean canStart = (auction.getStatus() == AuctionStatus.WAITING || auction.getStatus() == AuctionStatus.SCHEDULED);
            boolean canCancel = (auction.getStatus() == AuctionStatus.WAITING || auction.getStatus() == AuctionStatus.SCHEDULED || auction.getStatus() == AuctionStatus.ACTIVATED);
            
            startNowButton.setVisible(canStart);
            startNowButton.setManaged(canStart);
            cancelAuctionButton.setVisible(canCancel);
            cancelAuctionButton.setManaged(canCancel);
            
            // Allow seller to view room (but they can't bid)
            viewAuctionButton.setVisible(true);
            viewAuctionButton.setManaged(true);
        } else {
            // I'm a Buyer
            subcribeToggle.setVisible(true);
            subcribeToggle.setManaged(true);
            
            startNowButton.setVisible(false);
            startNowButton.setManaged(false);
            cancelAuctionButton.setVisible(false);
            cancelAuctionButton.setManaged(false);

            // Allow buyer to view room if it's active or finished
            boolean canView = (auction.getStatus() == AuctionStatus.ACTIVATED || auction.getStatus() == AuctionStatus.ENDED || auction.getStatus() == AuctionStatus.FINISHED);
            viewAuctionButton.setVisible(canView);
            viewAuctionButton.setManaged(canView);
        }
    }

    @FXML
    private void handleViewAuction(ActionEvent event) {
        RequestSender.send(new JoinAuctionRequest(auction.getId()));
    }

    @FXML
    private void handleSubscribeToggle(ActionEvent event) {
        if (subcribeToggle.isSelected()) {
            subcribeToggle.setText("UNFOLLOW");
            RequestSender.send(new JoinAuctionRequest(auction.getId()));
        } else {
            subcribeToggle.setText("FOLLOW NOW !");
            AuctionEventBus.getInstance().unsubscribe(auction.getId(), this);
        }
    }

    @FXML
    private void handleStartNow(ActionEvent event) {
        startNowButton.setDisable(true);
        cancelAuctionButton.setDisable(true);
        RequestSender.send(new ChangeAuctionStatusRequest(auction.getId(), AuctionStatus.ACTIVATED));
    }

    @FXML
    private void handleCancelAction(ActionEvent event) {
        startNowButton.setDisable(true);
        cancelAuctionButton.setDisable(true);
        RequestSender.send(new ChangeAuctionStatusRequest(auction.getId(), AuctionStatus.CANCELLED));
    }

    private void handleStatusChangeResponse(ChangeAuctionStatusResponse response) {
        if (auction == null || response.getUpdatedAuction() == null) return;
        if (response.getUpdatedAuction().getId() != auction.getId()) return; // Ignore other auctions

        Platform.runLater(() -> {
            if (response.isSuccess()) {
                this.auction = response.getUpdatedAuction();
                updateUI();
            } else {
                AlertUtils.showError("Action Failed", response.getMessage());
            }
            startNowButton.setDisable(false);
            cancelAuctionButton.setDisable(false);
        });
    }

    private void handleJoinAuctionResponse(JoinAuctionResponse response) {
        if (!response.isSuccess()) {
            Platform.runLater(() -> {
                subcribeToggle.setSelected(false);
                subcribeToggle.setText("FOLLOW NOW !");
                AlertUtils.showError("Follow Failed", "Could not follow auction.");
            });
        }
    }

    private void handleGetDetailAuction(GetAuctionDetailResponse response) {
        // Only navigate if it matches THIS card's ID
        if (response.isSuccess() && response.getAuctionDetail().getAuction().getId() == auction.getId()) {
            Platform.runLater(() -> {
                SessionManager.INSTANCE.setCurrentAuctionDetail(response.getAuctionDetail());
                SceneNavigator.INSTANCE.goTo("/com/groupproject/client/FXML/auctionscreen.fxml");
            });
        }
    }

    @Override
    public void onBidUpdated(NewBidEvent event) {
        Platform.runLater(() -> {
            auction.setCurrentBid(event.getNewBidAmount());
            currentprice.setText(String.format("$%.2f", event.getNewBidAmount()));
            currentprice.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 16px; -fx-font-weight: bold;");
        });
    }

    private String statusLabel(AuctionStatus status) {
        return switch (status) {
            case WAITING -> "⏳ Chờ bắt đầu";
            case SCHEDULED -> "📅 Đã lên lịch";
            case ACTIVATED -> "🟢 Đang diễn ra";
            case ENDED -> "🔴 Đã kết thúc";
            case CANCELLED -> "❌ Đã hủy";
            case FINISHED -> "✅ Hoàn thành";
        };
    }

    @Override
    public void cleanup() {
        ClientMessageRouter.INSTANCE.offResponse(GetAuctionDetailResponse.class, getDetailListener);
        ClientMessageRouter.INSTANCE.offResponse(JoinAuctionResponse.class, joinAuctionListener);
        ClientMessageRouter.INSTANCE.offResponse(ChangeAuctionStatusResponse.class, statusChangeListener);
        
        countDownHelper.stop();
        if (auction != null) {
            AuctionEventBus.getInstance().unsubscribe(auction.getId(), this);
        }
    }
}