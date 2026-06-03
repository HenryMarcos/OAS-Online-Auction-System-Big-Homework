package com.groupproject.client.utils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import com.groupproject.shared.model.transaction.Auction;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;

public class CountDownHelper {
    private Timeline timeline;
    private List<Label> labels;
    private Runnable onFinished;

    public void start(Auction auction, Runnable onFinished, Label... labelsToUpdate) {
        if (auction == null || auction.getEndTime() == null) return;
        
        this.labels = Arrays.asList(labelsToUpdate);
        this.onFinished = onFinished;

        if (timeline != null) { 
            timeline.stop(); 
        }

        timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            updateCountDown(auction.getEndTime());
        }));
        
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        
        updateCountDown(auction.getEndTime()); // Initial immediate update
    }

    public void stop() {
        if (timeline != null) timeline.stop();
    }

    private void updateCountDown(LocalDateTime endDate) {
        // Delegate formatting and syncing entirely to TimeUtil!
        String timeText = TimeUtil.formatTimeRemaining(endDate);
        
        for (Label label : labels) {
            label.setText(timeText);
        }

        if ("ENDED".equals(timeText)) {
            stop();
            if (onFinished != null) onFinished.run();
        }
    }
}