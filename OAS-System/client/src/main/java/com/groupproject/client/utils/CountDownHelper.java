package com.groupproject.client.utils;
import java.time.Duration;
import java.time.LocalDateTime;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;

import com.groupproject.shared.model.transaction.AuctionItem;
import com.groupproject.shared.model.transaction.Auction;
import java.util.Arrays;
import java.util.List;

// Có chức năng hiển thị ngày giờ , phút , giây cho từng item trong home nen lay thoi gian tu sever
// co chuc nang vo hieu hoa khi thoi gian het gio 
public class CountDownHelper {
    private Timeline timeline;
    private Auction auction;
    private AuctionItem auctionItem;
    private List<Label> labels;
    private Runnable onFinished;

    // bắt đầu thời gian đém ngược
    public void start(Auction auction, Runnable onFinished, Label... labelsToUpdate) {
        this.auction = auction;
        this.labels = Arrays.asList(labelsToUpdate);
        this.onFinished = onFinished;

        if (timeline != null) { 
            timeline.stop(); 
        }

        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            updateCountDown();
        }));
        
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        
        updateCountDown(); // Gọi ngay để hiện số lập tức 
    }

    public void start(AuctionItem auctionItem, Runnable onFinished, Label... labelsToUpdate) {
        this.auctionItem = auctionItem;
        this.labels = Arrays.asList(labelsToUpdate);
        this.onFinished = onFinished;

        if (timeline != null) { 
            timeline.stop(); 
        }

        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            updateCountDown();
        }));
        
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        
        updateCountDown(); // Gọi ngay để hiện số lập tức 
    }

    public void stop() {
        if (timeline != null) timeline.stop();
    }

    // hiện thị ra màn hình ngày , giờ, phút, giây (phía client, sever sẽ xử lý sau)
    private void updateCountDown() {
        if (auction == null || auction.getEndDate() == null) return;

        String timeText = formatRemainingTime(auction.getEndDate());
        labels.forEach(label -> label.setText(timeText));

        if (timeText.equals("ENDED")) {
            stop();
            if (onFinished != null) onFinished.run();
        }
    }

    public static String formatRemainingTime(LocalDateTime endDate) {
        Duration duration = Duration.between(LocalDateTime.now(), endDate);
        if (duration.isNegative() || duration.isZero()) {
            return "ENDED";
        } else {
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.getSeconds() % 60;
            return String.format("%dd : %02dh : %02dm : %02ds", days, hours, minutes, seconds);
        }
    }
}
