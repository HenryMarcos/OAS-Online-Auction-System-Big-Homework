package com.groupproject.client;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.groupproject.client.network.AuctionEventBus;
import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.client.utils.TimeUtil;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.events.NewBidEvent;
import com.groupproject.shared.network.requests.JoinAuctionRequest;
import com.groupproject.shared.network.requests.UnwatchAuctionRequest;
import com.groupproject.shared.network.requests.WatchAuctionRequest;
import com.groupproject.shared.network.responses.UnwatchAuctionResponse;
import com.groupproject.shared.network.responses.WatchAuctionResponse;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;

public class TestCardController implements LifecycleController {
    private Auction auction;
    private Timeline timeline;

    private final java.util.function.Consumer<NewBidEvent> newBidEventListener = this::handleNewBidEvent;
    private final java.util.function.Consumer<WatchAuctionResponse> watchResponseListener = this::handleWatchResponse;
    private final java.util.function.Consumer<UnwatchAuctionResponse> unwatchResponseListener = this::handleUnwatchResponse;

    // Màu sắc nút
    private static final String STYLE_WATCHING =
            "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;";
    private static final String STYLE_NOT_WATCHING =
            "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;";

    @FXML private ImageView image;
    @FXML private Label productname;
    @FXML private Label currentprice;
    @FXML private Label timeleft;
    @FXML private ToggleButton watchButton;

    // Gọi hàm này từ HomeController để thêm dữ liệu vào card
    public void setAuction(Auction auction) {
        this.auction = auction;

        productname.setText(auction.getTitle());

        double displayPrice = (auction.getCurrentBid() > 0) ? auction.getCurrentBid() : auction.getStartingPrice();
        currentprice.setText("$" + displayPrice);

        // Setup timer đếm ngược
        if (auction.getEndTime() != null) {
            timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> updateCountDown()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            updateCountDown();
        } else {
            timeleft.setText("No End Time");
        }

        // Đồng bộ trạng thái nút từ SessionManager (fix Q1)
        boolean alreadyWatching = SessionManager.INSTANCE.isWatchingAuction(auction.getId());
        applyWatchingState(alreadyWatching);

        // Đăng ký lắng nghe NewBidEvent để cập nhật giá realtime trên card
        ClientMessageRouter.INSTANCE.onEvent(NewBidEvent.class, newBidEventListener);

        // Đăng ký lắng nghe phản hồi từ server cho Watch/Unwatch
        ClientMessageRouter.INSTANCE.onResponse(WatchAuctionResponse.class, watchResponseListener);
        ClientMessageRouter.INSTANCE.onResponse(UnwatchAuctionResponse.class, unwatchResponseListener);


    }

    // --- Nút Bid Now → vào màn hình đấu giá ---
    @FXML
    private void handleBid(ActionEvent event) {
        JoinAuctionRequest request = new JoinAuctionRequest(auction.getId());
        RequestSender.send(request);
        // Chờ JoinAuctionResponse từ MainController/Router → điều hướng màn hình
    }

    // --- Nút Đăng ký / Hủy đăng ký ---
    @FXML
    private void handleWatchToggle(ActionEvent event) {
        boolean nowSelected = watchButton.isSelected();
        // Disable tạm để tránh double-click
        watchButton.setDisable(true);

        if (nowSelected) {
            // Người dùng vừa bấm "Đăng ký"
            RequestSender.send(new WatchAuctionRequest(auction.getId()));
        } else {
            // Người dùng vừa bấm "Hủy đăng ký"
            RequestSender.send(new UnwatchAuctionRequest(auction.getId()));
        }
    }

    // --- Xử lý phản hồi từ server ---
    private void handleWatchResponse(WatchAuctionResponse response) {
        if (auction == null || response.getAuctionId() != auction.getId()) return;

        Platform.runLater(() -> {
            watchButton.setDisable(false);
            if (response.isSuccess()) {
                SessionManager.INSTANCE.addWatchedAuction(auction.getId());
                applyWatchingState(true);
            } else {
                // Revert toggle nếu server từ chối
                watchButton.setSelected(false);
            }
        });
    }

    private void handleUnwatchResponse(UnwatchAuctionResponse response) {
        if (auction == null || response.getAuctionId() != auction.getId()) return;

        Platform.runLater(() -> {
            watchButton.setDisable(false);
            if (response.isSuccess()) {
                SessionManager.INSTANCE.removeWatchedAuction(auction.getId());
                applyWatchingState(false);
            } else {
                // Revert toggle
                watchButton.setSelected(true);
            }
        });
    }

    // --- Nhận NewBidEvent: cập nhật giá, nếu bị outbid thì reset nút Watch ---
    private void handleNewBidEvent(NewBidEvent event) {
        if (auction == null || event.getAuctionId() != auction.getId()) return;

        // Cập nhật giá hiển thị
        Platform.runLater(() -> currentprice.setText("$" + event.getNewBidAmount()));

        // Nếu mình đang theo dõi phòng này và bị người khác vượt qua (outbid)
        // → Server đã unsubscribe mình khỏi phòng, đồng bộ lại UI
        int currentUserId = SessionManager.INSTANCE.getCurrentUser() != null
                ? SessionManager.INSTANCE.getCurrentUser().getId() : -1;

        if (SessionManager.INSTANCE.isWatchingAuction(auction.getId())
                && event.getHighestBidderId() != currentUserId) {
            SessionManager.INSTANCE.removeWatchedAuction(auction.getId());
            Platform.runLater(() -> applyWatchingState(false));
        }
    }

    // --- Helper: áp dụng style và text cho nút theo trạng thái ---
    private void applyWatchingState(boolean watching) {
        watchButton.setSelected(watching);
        if (watching) {
            watchButton.setText("Hủy đăng ký");
            watchButton.setStyle(STYLE_WATCHING);
        } else {
            watchButton.setText("Đăng ký");
            watchButton.setStyle(STYLE_NOT_WATCHING);
        }
    }

    // --- Đếm ngược ---
    public void updateCountDown() {
        if (auction.getEndTime() == null) return;

        LocalDateTime syncedNow = TimeUtil.getNow();
        Duration remaining = Duration.between(syncedNow, auction.getEndTime());

        if (remaining.isNegative() || remaining.isZero()) {
            timeleft.setText("ENDED");
            if (timeline != null) timeline.stop();
        } else {
            long totalSeconds = ChronoUnit.SECONDS.between(syncedNow, auction.getEndTime());
            long days    = totalSeconds / 86400;
            long hours   = (totalSeconds % 86400) / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            timeleft.setText(String.format("Ending in: %dd : %02dh : %02dm : %02ds", days, hours, minutes, seconds));
        }
    }

    // --- Dừng timer khi rời Home screen ---
    @Override
    public void cleanup() {
        if (timeline != null) timeline.stop();
        ClientMessageRouter.INSTANCE.offEvent(NewBidEvent.class, newBidEventListener);
        ClientMessageRouter.INSTANCE.offResponse(WatchAuctionResponse.class, watchResponseListener);
        ClientMessageRouter.INSTANCE.offResponse(UnwatchAuctionResponse.class, unwatchResponseListener);
    }
}
