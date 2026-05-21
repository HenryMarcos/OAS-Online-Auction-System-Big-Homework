package com.groupproject.client;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.groupproject.client.network.AuctionIntegrationService;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.CountDownHelper;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.transaction.AuctionDetail;
import com.groupproject.shared.model.transaction.BidTransaction;
import com.groupproject.shared.network.AuctionEvent.*;
import com.groupproject.shared.network.BidRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
public class AuctionController implements AuctionListener  {
    private AuctionDetail currentAuctionDetail = SessionManager.getInstance().getCurrentAuctionDetail();
    private final ObservableList<BidTransaction> bidDataList= FXCollections.observableArrayList();
    private AuctionIntegrationService integrationService;
    @FXML private LineChart<String,Number> linechart;
    private Series<String, Number> priceSeries = new XYChart.Series<>() ;
    @FXML private TableView<BidTransaction> bottomtable;
    @FXML private TableColumn<BidTransaction, Double> pricecol;
    @FXML private TableColumn<BidTransaction, String> timecol;
    @FXML private TableColumn<BidTransaction,Integer> userIdcol;
    @FXML private Button bidButton;
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
        if (integrationService != null) {
            integrationService.stopListening();
        }
    }
    @FXML
    public  void initialize() {
        int id = currentAuctionDetail.getAuction().getId().intValue();
        this.integrationService = new AuctionIntegrationService(id, this);
        this.integrationService.startListening();
        setAuction(currentAuctionDetail.getAuction());
        // Cài đặt bảng 
        setUpTableView();
        // Cài đặt linechart 
        linechart.getData().add(priceSeries);
        // lắng nghe tín hiệu từ Sever 
    }
    @Override 
    public void onBidUpdated(BidUpdatedEvent event) {
        Platform.runLater(() -> {
            String priceText = String.format("Current price : %.2f VND", event.getBidAmount());
            auctioncurrentprice.setText(priceText);
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            AlertUtils.showSuccess("Success", "Someone places bid successfully");
            bidButton.setDisable(true);
            bidButton.setText("PLACE BID");
        });
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
        String priceText = String.format("Current price : %.2f VND",currentAuctionDetail.getAuction().getCurrentBid());
        auctioncurrentprice.setText(priceText);
    }

    private void updateName() {
        String name = "Name : " + currentAuctionDetail.getAuction().getTitle();
        auctionproductname.setText(name);
        participant.setText(SessionManager.getInstance().getCurrentUser().getUsername());
    }
    private void updateTime() {
        CountDownHelper countDownHelper = new CountDownHelper();
        countDownHelper.start(currentAuctionDetail, () -> auctiontimeleft.setText("ENDED"), auctiontimeleft);
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
            if (price <= currentAuctionDetail.getAuction().getCurrentBid() ) {
                AlertUtils.showError("Lỗi logic","Giá đặt phải cao hơn giá hiện tại");
                return;
            }
            String username= SessionManager.getInstance().getCurrentUser().getUsername();
            BidRequest request = new BidRequest(currentAuctionDetail.getAuction().getId().intValue(), username, price);
            RequestSender.send(request);
            enterprice.clear();
            bidButton.setDisable(false);
            bidButton.setText("Loading...");
            
        }
        catch (NumberFormatException e ) {
            AlertUtils.showError("Lỗi định dạng", "Vui lòng chỉ nhập số");
        }
    }

    private void setUpTableView() {
        userIdcol.setCellValueFactory(new PropertyValueFactory<>("bidderId"));
        pricecol.setCellValueFactory(new PropertyValueFactory<>("price"));
        timecol.setCellValueFactory(new PropertyValueFactory<>("time"));
        bottomtable.setItems(bidDataList);
    }
    /* 
    private void updateAuctionUI() {
        // CHÚ Ý: Vì gói tin mạng chạy ở luồng ngầm (Background Thread),
        // bắt buộc phải dùng Platform.runLater để can thiệp vào giao diện (UI Thread) nhằm tránh crash.
        Platform.runLater(() -> {
            
            // 1. Thêm lượt đặt giá mới vào ĐẦU danh sách (vị trí số 0) 
            // Điều này giúp lượt đặt giá mới nhất luôn nhảy lên dòng ĐẦU TIÊN của bảng để dễ nhìn.
            bidDataList.add(0,new BidTransaction(id, currentbid,time));

            // 2. GIẢI QUYẾT YÊU CẦU CỦA BẠN: Kiểm tra và xóa bớt phần tử thừa để tránh lãng phí bộ nhớ
            // Sử dụng vòng lặp while để đảm bảo danh sách không bao giờ vượt quá ngưỡng quy định.
            while (bidDataList.size() > MAX_BIDS_TO_DISPLAY) {
                
                // Vì ta thêm phần tử mới vào đầu (vị trí 0), nên phần tử CŨ NHẤT 
                // sẽ luôn bị đẩy về vị trí CUỐI CÙNG (index bằng size - 1).
                int oldestItemIndex = bidDataList.size() - 1;
                
                // Xóa bỏ nó khỏi danh sách
                bidDataList.remove(oldestItemIndex);
            }
            
            // 3. Tự động cuộn bảng lên trên cùng để xem dòng mới nhất vừa nhảy vào
            bottomtable.scrollTo(0);
        });
        thêm vào khi có một lượt đặt giá mới 
        duyệt ngay từ đầu khi vào một phiên đấu giá 
        public void loadInitialBidHistory(List<BidTransaction> serverHistory) {
        if (serverHistory == null) return;

        bidDataList.clear();

        // Kiểm tra tối ưu ngay từ lúc nạp dữ liệu ban đầu
        if (serverHistory.size() > MAX_BIDS_TO_DISPLAY) {
            // Nếu Server trả về quá nhiều (ví dụ 1000 dòng), ta chỉ cắt lấy 30 dòng mới nhất đưa vào UI
            List<BidTransaction> truncatedList = serverHistory.subList(0, MAX_BIDS_TO_DISPLAY);
            bidDataList.addAll(truncatedList);
            ClientLogger.info("Đã cắt bớt lịch sử đấu giá ban đầu để tối ưu RAM.");
        } else {
            bidDataList.addAll(serverHistory);
        }
    }

    */

    @Override 
    public void onAuctionCancelled(AuctionCancelledEvent event) {
        Platform.runLater(() -> {
            AlertUtils.showAlert(Alert.AlertType.INFORMATION, "THÔNG BÁO", event.getReason());
            bidButton.setDisable(false);

        });
        
    }
    @Override
    public void onAuctionFinished(AuctionFinisedEvent event) {
        Platform.runLater(() -> {
            AlertUtils.showAlert(Alert.AlertType.INFORMATION, "THÔNG BÁO", "PHIÊN ĐẤU GIÁ ĐÃ ĐƯỢC HOÀN THÀNH");
            bidButton.setDisable(false);

        });
    }
    @Override 
    public void onAuctionStarted(AuctionStartedEvent event) {
        Platform.runLater(() -> {
            AlertUtils.showAlert(Alert.AlertType.INFORMATION,"THÔNG BÁO"," PHIÊN ĐẤU GIÁ BẮT ĐẦU ");
            bidButton.setDisable(true);
        });
    }
    @Override 
    public void onAuctionEnded(AuctionEndedEvent event) {
        Platform.runLater(() -> {
            AlertUtils.showAlert(Alert.AlertType.INFORMATION,"THÔNG BÁO"," PHIÊN ĐẤU GIÁ KẾT THÚC ! CHỜ THANH TOÁN ");
            bidButton.setDisable(false);
        });
    }

}
