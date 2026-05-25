package com.groupproject.client;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.JoinAuctionRequest;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class TestCardController implements LifecycleController {
    private Auction auction;
    private Timeline timeline;

    @FXML private ImageView image;
    @FXML private Label productname;
    @FXML private Label currentprice;
    @FXML private Label timeleft;

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
        if (auction.getEndTime() == null) return;

        Duration remaining = Duration.between(LocalDateTime.now(), auction.getEndTime());
        
        if (remaining.isNegative() || remaining.isZero()) {
            timeleft.setText("ENDED");
            if (timeline != null) timeline.stop();
        } else {
            long totalSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
            long days    = totalSeconds / 86400;           
            long hours   = (totalSeconds % 86400) / 3600;  
            long minutes = (totalSeconds % 3600) / 60;     
            long seconds = totalSeconds % 60;              

            timeleft.setText(String.format("Ending in: %dd : %02dh : %02dm : %02ds", days, hours, minutes, seconds));
        }
    }

    @FXML 
    private void handleBid(ActionEvent event) {
        int currentUserId = SessionManager.INSTANCE.getCurrentUser().getId();
        
        // Send the request to the server to join this specific auction
        JoinAuctionRequest request = new JoinAuctionRequest(auction.getId(), currentUserId);
        RequestSender.send(request);
        
        // We do NOT change screens here. We wait for the server to reply!
    }

    // Stop the timer when the user leaves the home screen!
    @Override
    public void cleanup() {
        if (timeline != null) {
            timeline.stop();
        }
    }
}
