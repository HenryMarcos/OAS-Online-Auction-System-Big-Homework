package com.groupproject.client;

import com.groupproject.client.network.AuctionEventBus;
import com.groupproject.client.network.AuctionListener;
import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.CountDownHelper;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.events.NewBidEvent;
import com.groupproject.shared.network.requests.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.responses.ChangeAuctionStatusResponse;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

// 🌟 OPTIMIZATION 1: Implements AuctionListener so the seller sees live price updates
public class MyAuctionCardController implements LifecycleController, AuctionListener {

    private Auction auction;
    private final java.util.function.Consumer<ChangeAuctionStatusResponse> statusChangeListener = this::handleStatusChangeResponse;
    private CountDownHelper countDownHelper = new CountDownHelper();

    @FXML private ImageView image;
    @FXML private Label productname;
    @FXML private Label auctionStatus;
    @FXML private Label currentprice;
    @FXML private Label timeleft;
    @FXML private Button startNowButton;
    @FXML private Button cancelAuctionButton;

    public void setAuction(Auction auction) {
        this.auction = auction;
        updateUI(auction);

        // 🌟 Subscribe to global responses
        ClientMessageRouter.INSTANCE.onResponse(ChangeAuctionStatusResponse.class, statusChangeListener);
        
        // 🌟 Subscribe to local room updates (Live Bids!)
        AuctionEventBus.getInstance().subscribe(auction.getId(), this);
    }

    private void updateUI(Auction auc) {
        productname.setText(auc.getTitle());
        
        double displayPrice = (auc.getCurrentBid() > 0) ? auc.getCurrentBid() : auc.getStartingPrice();
        currentprice.setText(String.format("$%.2f", displayPrice));
        
        auctionStatus.setText(statusLabel(auc.getStatus()));

        // Start countdown using our newly refactored helper
        countDownHelper.start(auc, () -> {
            Platform.runLater(() -> updateUI(this.auction));
        }, timeleft);

        // UI Logic for buttons based on status
        boolean canStart = (auc.getStatus() == AuctionStatus.WAITING || auc.getStatus() == AuctionStatus.SCHEDULED);
        boolean canCancel = (auc.getStatus() == AuctionStatus.WAITING || auc.getStatus() == AuctionStatus.SCHEDULED || auc.getStatus() == AuctionStatus.ACTIVATED);
        
        startNowButton.setVisible(canStart);
        startNowButton.setManaged(canStart);
        cancelAuctionButton.setVisible(canCancel);
        cancelAuctionButton.setManaged(canCancel);
    }

    @FXML
    private void handleStartNow(ActionEvent event) {
        startNowButton.setDisable(true);
        cancelAuctionButton.setDisable(true);
        RequestSender.send(new ChangeAuctionStatusRequest(auction.getId(), AuctionStatus.ACTIVATED));
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        startNowButton.setDisable(true);
        cancelAuctionButton.setDisable(true);
        RequestSender.send(new ChangeAuctionStatusRequest(auction.getId(), AuctionStatus.CANCELLED));
    }

    private void handleStatusChangeResponse(ChangeAuctionStatusResponse response) {
        if (auction == null || response.getUpdatedAuction() == null) return;
        
        // 🌟 OPTIMIZATION 2: If this response is for a DIFFERENT auction, DO NOTHING!
        if (response.getUpdatedAuction().getId() != auction.getId()) {
            return; // Ignore entirely. Do not re-enable buttons.
        }

        Platform.runLater(() -> {
            if (response.isSuccess()) {
                this.auction = response.getUpdatedAuction();
                updateUI(this.auction);
            } else {
                AlertUtils.showError("Action Failed", response.getMessage());
            }
            // Safely re-enable buttons for THIS specific card
            startNowButton.setDisable(false);
            cancelAuctionButton.setDisable(false);
        });
    }

    // 🌟 OPTIMIZATION 3: Implement Live Bidding update logic
    @Override
    public void onBidUpdated(NewBidEvent event) {
        Platform.runLater(() -> {
            auction.setCurrentBid(event.getNewBidAmount());
            currentprice.setText(String.format("$%.2f", event.getNewBidAmount()));
            
            // Add a brief visual highlight to let the seller know they got a bid!
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
        // 🌟 OPTIMIZATION 4: Stop timer and aggressively unsubscribe to prevent memory leaks
        countDownHelper.stop();
        ClientMessageRouter.INSTANCE.offResponse(ChangeAuctionStatusResponse.class, statusChangeListener);
        
        if (auction != null) {
            AuctionEventBus.getInstance().unsubscribe(auction.getId(), this);
        }
    }
}