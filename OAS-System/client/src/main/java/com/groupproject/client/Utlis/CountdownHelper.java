package com.groupproject.client.Utlis;
import java.time.Duration;
import java.time.LocalDateTime;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
// Có chức năng hiển thị ngày giờ , phút , giây cho từng item trong home nen lay thoi gian tu sever
// co chuc nang vo hieu hoa khi thoi gian het gio 
public class CountdownHelper {
    private Timeline timeline;
    private Label timeleft; // Lưu lại để hàm updateCountDown dùng được
    private Item item;      // Lưu lại để hàm updateCountDown dùng được
    // bắt đầu thời gian đém ngược
    public void start(Item item, Label timeleft) {
        this.item = item;
        this.timeleft = timeleft;

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
    // hiện thị ra màn hình ngày , giờ, phút, giây (phía client, sever sẽ xử lý sau)
    private void updateCountDown() {
        Duration remaining = Duration.between(LocalDateTime.now(), item.getEndDate());
        if (remaining.isNegative() || remaining.isZero()) {
            timeleft.setText("ENDED");
            timeline.stop();
            if (timeline != null) timeline.stop();
        } else {
            
            long days = remaining.toDays();
            long hours = remaining.toHours() % 24;
            long minutes = remaining.toMinutes() % 60;
            long seconds = remaining.getSeconds() % 60;

            timeleft.setText(String.format("Ending in: %dd : %02dh : %02dm : %02ds", 
                             days, hours, minutes, seconds));
        }
    }
}