package com.groupproject.client;
import com.groupproject.client.Data.*;

import java.io.IOException;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javafx.scene.control.Label;

import com.groupproject.client.network.EventRouter;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.shared.AuctionUpdate;

import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.chart.LineChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.AuctionUpdate;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.BidRequest;
import com.groupproject.shared.network.Response;
import com.groupproject.shared.transaction.Auction;

import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
;
public class AuctionController implements Initializable {
    @FXML private LineChart<String,Number> linechart;
    private Series<String, Number> priceSeries = new XYChart.Series<>() ;
    @FXML private TableView<BidRecord> bottomtable;
    @FXML private TableColumn<BidRecord, String> usercol;
    @FXML private TableColumn<BidRecord, Double> pricecol;
    @FXML private TableColumn<BidRecord, String> timecol;
    @FXML private TableColumn<BidRecord,Integer> idusercol;
    private Auction currentAuction;
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
    private Label auctionsession;
    @FXML 
    private void switchtoHome(ActionEvent event) throws IOException {
        SceneNavigator.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
    }

    @Override
    public  void initialize(URL location, ResourceBundle resources) {
        // set up name 
        String name  = SessionManager.getInstance().getCurrentUser().getUsername();
        participant.setText(name);
        // set up ten phien dau gia : duoc lay tu databse 

        // Cài đặt bảng 
        setUpTableView();
        // Cài đặt linechart 
        linechart.getData().add(priceSeries);
        // Khởi động đồng hồ đếm ngược 
        startCountDown();
        // lắng nghe tín hiệu từ Sever 
        //listenForSeverUpdate();

    }
    // ve sau se duoc thay the bang viec lay tren databse xuong ( thay the tu dong 90 - 150)
    public void setItem(Item item, Label currentprice,Label timeleft, Label productname) {
        this.item= item;
        this.currentprice=currentprice;
        this.timeleft= timeleft;
        this.productname=productname;
        startprice.setText(String.format("Starting price : %.0f VND ",item.getStartingPrice()));
        updateName();
        updatePrice();
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
        String text = enterprice.getText().trim();
        if (text.isEmpty()) {
            AlertUtils.showError("Lỗi nhập liệu ! ","Vui lòng nhập số tiền mà bạn muốn");
            return;
        }
        try {
            double price = Double.parseDouble(text);
            if (price <= 0) {
                AlertUtils.showError("Lỗi logic","Số tiền đấu giá phải lớn hơn 0");
                return;
            }
            // Kiểm tra xem giá vừa nhập đang cao hơn giá hiện tại không ? 
            if (price <= currentAuction.getCurrentBid() ) {
                AlertUtils.showError("Lỗi logic","Giá đặt phải cao hơn giá hiện tại");
                return;
            }
            // lấy những trường thông tin cần thiết để gửi BidRequest lên Sever
            String username= SessionManager.getInstance().getCurrentUser().getUsername();
            String currentAuctionId = currentAuction.getId();
            BidRequest request = new BidRequest(currentAuctionId,username,price);

        }
        catch (NumberFormatException e ) {
            AlertUtils.showError("Lỗi định dạng", "Vui lòng chỉ nhập số");
        }

    }
    private void setUpTableView() {
        idusercol.setCellValueFactory(new PropertyValueFactory<>("id"));
        usercol.setCellValueFactory(new PropertyValueFactory<>("bidder"));
        pricecol.setCellValueFactory(new PropertyValueFactory<>("price"));
        timecol.setCellValueFactory(new PropertyValueFactory<>("time"));
    }
    /* 
    private void listenForSeverUpdate() {
        EventRouter.getInstance().on(AuctionUpdate.class, update-> {
                Platform.runLater(()-> {
                    String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    updateAuctionUI(update.getBidderid(),update.getBidderUsername(),update.getCurrentBid(),currentTime);
                });
        });
    }
    private void updateAuctionUI(int id, String name, double currentbid, String time ) {

    }
    */
    
}


