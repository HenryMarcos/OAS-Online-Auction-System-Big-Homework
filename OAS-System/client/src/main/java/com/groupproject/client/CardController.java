package com.groupproject.client;
import com.groupproject.client.Data.*;

import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.image.ImageView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;
public class CardController { 
    private Item item;
    private Timeline timeline;
    @FXML
    private ImageView image;
    @FXML
    private Label productname;
    @FXML
    private Label currentprice;
    @FXML
    private Label timeleft;
    @FXML 
    private void handleBid(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML/auctionscreen.fxml"));
        Parent root = (Parent) loader.load();
        AuctionController auctioncontroller= loader.getController();
        auctioncontroller.setItem(item,currentprice,timeleft,productname);
        Scene scene = new Scene(root,1000,700);
        scene.getStylesheets().add(getClass().getResource("/com/groupproject/client/CSS/auctionscreen.css").toExternalForm());
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Auction Screen | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(scene);
        currentStage.show();
    }
    public void setItem(Item item) {
        this.item=item;
        productname.setText("Name: " + item.getName());
        currentprice.setText("Current price: " + item.getCurrentPrice());
        startCountDown();
    }
    public void startCountDown() {
        if (timeline != null) {
            timeline.stop();
        }
        else {
            timeline= new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
                updateCountDown();
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            updateCountDown();
        }
    }
    public void updateCountDown() {
        if( item.getEndDate() == null ) {
            return;
        }
        else {
            Duration remaining= Duration.between(LocalDateTime.now(),item.getEndDate());
            if (remaining.isNegative() || remaining.isZero()) {
                timeleft.setText("ENDED");
                timeline.stop();
                return;
            }
            else {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime end = item.getEndDate();
                long totalSeconds = ChronoUnit.SECONDS.between(now, end);

    // Tách ra từng đơn vị bằng cách chia lấy dư
                long days    = totalSeconds / 86400;           // 1 ngày = 86400 giây
                long hours   = (totalSeconds % 86400) / 3600;  // Phần dư sau ngày / 3600
                long minutes = (totalSeconds % 3600) / 60;     // Phần dư sau giờ / 60
                long seconds = totalSeconds % 60;              // Phần dư sau phút

                timeleft.setText(String.format("Ending in: %dd : %02dh : %02dm : %02ds",
                                                days, hours, minutes, seconds));
            }
        }
    }
    
}
