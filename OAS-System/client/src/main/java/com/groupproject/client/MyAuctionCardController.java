package com.groupproject.client;

import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.CountDownHelper;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.responses.ChangeAuctionStatusResponse;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class MyAuctionCardController implements LifecycleController {

    private Auction auction;
    private final java.util.function.Consumer<ChangeAuctionStatusResponse> statusChangeListener = this::handleStatusChangeResponse;

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

        // Lắng nghe phản hồi ChangeAuctionStatus
        ClientMessageRouter.INSTANCE.onResponse(ChangeAuctionStatusResponse.class, statusChangeListener);
    }

    private void updateUI(Auction a) {
        Platform.runLater(() -> {
            productname.setText(a.getTitle());

            double price = a.getCurrentBid() > 0 ? a.getCurrentBid() : a.getStartingPrice();
            currentprice.setText(String.format("$%,.0f", price));
            auctionStatus.setText("Trạng thái: " + statusLabel(a.getStatus()));

            // Cập nhật đếm ngược nếu đang ACTIVED
            if (a.getEndTime() != null) {
                CountDownHelper helper = new CountDownHelper();
                helper.start(a, () -> timeleft.setText("ĐÃ KẾT THÚC"), timeleft);
            } else if (a.getStatus() == AuctionStatus.WAITING) {
                timeleft.setText("Chưa bắt đầu — nhấn 'Bắt đầu ngay'");
            } else if (a.getStatus() == AuctionStatus.SCHEDULED) {
                timeleft.setText("Lịch: " + (a.getStartTime() != null ? a.getStartTime().toString() : "—"));
            } else {
                timeleft.setText("");
            }

            applyButtonVisibility(a);
        });
    }

    private void applyButtonVisibility(Auction a) {
        int currentUserId = SessionManager.INSTANCE.getCurrentUser() != null
                ? SessionManager.INSTANCE.getCurrentUser().getId() : -1;
        boolean isSeller = (a.getSellerId() == currentUserId);

        // Nút "Bắt đầu ngay": CHỈ cho auction WAITING của chính mình
        boolean canStart = isSeller && a.getStatus() == AuctionStatus.WAITING;
        startNowButton.setVisible(canStart);
        startNowButton.setManaged(canStart);

        // Nút "Hủy phiên": cho WAITING hoặc SCHEDULED của chính mình
        boolean canCancel = isSeller &&
                (a.getStatus() == AuctionStatus.WAITING || a.getStatus() == AuctionStatus.SCHEDULED);
        cancelAuctionButton.setVisible(canCancel);
        cancelAuctionButton.setManaged(canCancel);
    }

    @FXML
    private void handleStartNow(ActionEvent event) {
        if (auction == null) return;
        startNowButton.setDisable(true);
        RequestSender.send(new ChangeAuctionStatusRequest(auction.getId(), AuctionStatus.ACTIVED));
    }

    @FXML
    private void handleCancelAuction(ActionEvent event) {
        if (auction == null) return;
        cancelAuctionButton.setDisable(true);
        RequestSender.send(new ChangeAuctionStatusRequest(auction.getId(), AuctionStatus.CANCELLED));
    }

    private void handleStatusChangeResponse(ChangeAuctionStatusResponse response) {
        if (auction == null) return;
        // Chỉ xử lý response cho auction của card này
        if (response.getUpdatedAuction() == null || response.getUpdatedAuction().getId() != auction.getId()) {
            // Re-enable nếu không phải auction này
            Platform.runLater(() -> {
                startNowButton.setDisable(false);
                cancelAuctionButton.setDisable(false);
            });
            return;
        }

        Platform.runLater(() -> {
            if (response.isSuccess()) {
                auction = response.getUpdatedAuction();
                updateUI(auction);
            } else {
                AlertUtils.showError("Lỗi", response.getMessage());
                startNowButton.setDisable(false);
                cancelAuctionButton.setDisable(false);
            }
        });
    }

    private String statusLabel(AuctionStatus status) {
        return switch (status) {
            case WAITING -> "⏳ Chờ bắt đầu";
            case SCHEDULED -> "📅 Đã lên lịch";
            case ACTIVED -> "🟢 Đang diễn ra";
            case ENDED -> "🔴 Đã kết thúc";
            case CANCELLED -> "❌ Đã hủy";
            case FINISHED -> "✅ Hoàn thành";
        };
    }

    @Override
    public void cleanup() {
        ClientMessageRouter.INSTANCE.offResponse(ChangeAuctionStatusResponse.class, statusChangeListener);
    }
}
