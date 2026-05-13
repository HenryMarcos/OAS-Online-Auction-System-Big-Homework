package com.groupproject.client;
import java.io.IOException;

import com.groupproject.client.Data.BidRecord;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.CountDownHelper;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.BidRequest;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
;
public class AuctionController  {
    @FXML private LineChart<String,Number> linechart;
    private Series<String, Number> priceSeries = new XYChart.Series<>() ;
    @FXML private TableView<BidRecord> bottomtable;
    @FXML private TableColumn<BidRecord, String> usercol;
    @FXML private TableColumn<BidRecord, Double> pricecol;
    @FXML private TableColumn<BidRecord, String> timecol;
    @FXML private TableColumn<BidRecord,Integer> idusercol;
    private Auction currentAuction = SessionManager.getInstance().getCurrentAuction();
    @FXML private TextField enterprice;
    @FXML private Label startprice;
    @FXML private ImageView productImageView;
    @FXML private Label auctiontimeleft;
    @FXML private Label auctioncurrentprice;
    @FXML private Label participant;
    @FXML private Label auctionproductname;
    @FXML private Label auctionsession;
    @FXML 
    private void switchtoHome(ActionEvent event) throws IOException {
        SceneNavigator.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
    }

    @FXML
    public  void initialize() {
        setAuction(currentAuction);
        // Cài đặt bảng 
        setUpTableView();
        // Cài đặt linechart 
        linechart.getData().add(priceSeries);
        // lắng nghe tín hiệu từ Sever 
        //listenForSeverUpdate();
    }

    // ve sau se duoc thay the bang viec lay tren databse xuong ( thay the tu dong 90 - 150)
    public void setAuction(Auction auction) {
        Platform.runLater(() -> {
            updateName();
            updatePrice();
            updateTime();
        });
    }

    private void updatePrice() {
        String priceText = String.format("Current price : %.2f VND", currentAuction.getCurrentBid());
        auctioncurrentprice.setText(priceText);
    }

    private void updateName() {
        String name = "Name : " + currentAuction.getItemName();
        auctionproductname.setText(name);
        participant.setText(SessionManager.getInstance().getCurrentUser().getUsername());
    }
    private void updateTime() {
        CountDownHelper countDownHelper = new CountDownHelper();
        countDownHelper.start(currentAuction, () -> auctiontimeleft.setText("ENDED"), auctiontimeleft);
    }

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
            String username= SessionManager.getInstance().getCurrentUser().getUsername();
            BidRequest request = new BidRequest(currentAuction.getId(), username, price);
            enterprice.clear();
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
