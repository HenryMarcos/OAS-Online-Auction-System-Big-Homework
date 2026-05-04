package com.groupproject.client;
import com.groupproject.client.Data.*;
import java.io.IOException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
public class AuctionController {
    private Item item;
    private Timeline timeline;
    private Label productname;
    private Label timeleft;
    private Label currentprice;
    @FXML
    private TextField enterprice;
    @FXML 
    private Label startprice;
    @FXML 
    private ImageView productImageView;
    @FXML
    private Label auctiontimeleft;
    @FXML
    private Label auctioncurrentprice;
    @FXML
    private Label participant;
    @FXML
    private Label auctionproductname;
    @FXML 
    private void switchtoHome(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/groupproject/client/FXML/mainscreen.fxml"));
    
        // Bước 2: Tạo một Scene (Cảnh diễn) mới từ giao diện vừa tải
        Scene newScene = new Scene(root,1000,700);
        newScene.getStylesheets().add(getClass().getResource("CSS/home.css").toExternalForm());
        // Bước 3: Lấy lại Sân khấu (Stage) hiện tại từ nút bấm mà người dùng vừa click
        Stage currentStage =  (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Home | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(newScene);
        currentStage.show();
    }
    public void setItem(Item item, Label currentprice,Label timeleft, Label productname) {
        this.item= item;
        this.currentprice=currentprice;
        this.timeleft= timeleft;
        this.productname=productname;
        startprice.setText(String.format("Starting price : %.0f VND ",item.getStartingPrice()));
        updateName();
        updatePrice();
        startCountDown();
    }
    private void updatePrice() {
        String priceText= String.format("Current price : %.2f VND",item.getCurrentPrice());
        auctioncurrentprice.setText(priceText);
        currentprice.setText(priceText);

    }
    private void updateName() {
        String name = "Name : " + item.getName();
        auctionproductname.setText(name);
        productname.setText(name);
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
                auctiontimeleft.setText("ENDED");
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

                auctiontimeleft.setText(String.format("Ending in: %dd : %02dh : %02dm : %02ds",
                                                days, hours, minutes, seconds));
            }
        }
    }
    // xu ly ngoai le khi co truong hop : khong ghi gi, ghi ca chu va so ,...
    @FXML
    private void handlePlaceBid() {
        double newBid= Double.parseDouble(enterprice.getText());
        if (newBid <= item.getCurrentPrice()) {
            return;
        }
        item.setCurrentPrice(newBid);
        updatePrice();
    }
    
}


