package com.groupproject.client;
import java.io.IOException;
import java.util.List;

import com.groupproject.client.network.AuctionIntegrationService;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.CountDownHelper;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.transaction.AuctionDetail;
import com.groupproject.shared.model.transaction.BidTransaction;
import com.groupproject.shared.network.AuctionEvent.AuctionCancelledEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionEndedEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionFinisedEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionListener;
import com.groupproject.shared.network.AuctionEvent.AuctionStartedEvent;
import com.groupproject.shared.network.AuctionEvent.BidUpdatedEvent;
import com.groupproject.shared.network.requests.BidRequest;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
// MAX_BIDS_TO_DISPLAY: có thể thêm nếu cần thiết bởi không cần thiết phải lấy hết tất cả mà chỉ cần lấy 10 người đứng đầu thôi chẳng hạn.
public class AuctionController implements AuctionListener  {
    private AuctionDetail currentAuctionDetail = SessionManager.INSTANCE.getCurrentAuctionDetail();
    private final ObservableList<BidTransaction> bidDataList= FXCollections.observableArrayList();
    private AuctionIntegrationService integrationService;
    private final int MAX_BIDS_TO_DISPLAY=20;
    @FXML private AreaChart<String,Number> priceChart;
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
        SceneNavigator.INSTANCE.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
        if (integrationService != null) {
            integrationService.stopListening();
        }
    }
    @FXML
    public  void initialize() {
        // cài đặt một cái được tích hợp để phát thông báo 
        Auction currentAuction= currentAuctionDetail.getAuction();
        int id = currentAuction.getId().intValue();
        this.integrationService = new AuctionIntegrationService(id, this);
        this.integrationService.startListening();
        setAuction(currentAuctionDetail.getAuction());
        // Cài đặt bảng 
        setUpTableView();
        // Cài đặt priceChart 
        priceChart.setLegendVisible(false);
        priceChart.getData().add(priceSeries);
        // Nhận dữ liệu để load những dữ liệu cũ  
        loadInitialData(currentAuctionDetail.getBidHistory());
    }
    @Override 
    public void onBidUpdated(BidUpdatedEvent event) {
        Platform.runLater(() -> {
            String priceText = String.format("Current price : %.2f VND", event.getBidAmount());
            auctioncurrentprice.setText(priceText);
            updateAuctionUI(event.getAuctionId(),event.getHighestBidderId(),event.getBidAmount());
            // Vẽ điểm trên đồ thị khi có BidUpdated 
            priceSeries.getData().add(new XYChart.Data<>(event.getTimeStamp(),event.getBidAmount()));
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
            if (currentAuctionDetail.getAuction().getStatus() == AuctionStatus.ACTIVATED) {
                bidButton.setDisable(false);
            }
            bidButton.setDisable(true);
        });
    }
    private void updatePrice() {
        String priceText = String.format("Current price : %.2f VND",currentAuctionDetail.getAuction().getCurrentBid());
        auctioncurrentprice.setText(priceText);
    }

    private void updateName() {
        String name = "Name : " + currentAuctionDetail.getAuction().getTitle();
        auctionproductname.setText(name);
        participant.setText(SessionManager.INSTANCE.getCurrentUser().getUsername());
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
            String username= SessionManager.INSTANCE.getCurrentUser().getUsername();
            BidRequest request = new BidRequest(currentAuctionDetail.getAuction().getId().intValue(), username, price);
            RequestSender.send(request);
            enterprice.clear();
            bidButton.setDisable(true);
            bidButton.setText("Loading...");
            
        }
        catch (NumberFormatException e ) {
            AlertUtils.showError("Lỗi định dạng", "Vui lòng chỉ nhập số");
        }
    }

    private void setUpTableView() {
        userIdcol.setCellValueFactory(celldata -> new SimpleObjectProperty<>(celldata.getValue().getBidderId()));
        pricecol.setCellValueFactory(celldata -> new SimpleObjectProperty<>(celldata.getValue().getBidAmount()));
        timecol.setCellValueFactory(celldata -> new SimpleObjectProperty<>(celldata.getValue().getTimeStamp()));
        bottomtable.setItems(bidDataList);
    }
    // Vẫn chưa điền dữ liệu vào những bảng cần điền ( đang mới chỉ lưu dữ liệu mà thôi)
    private void updateAuctionUI( int auctionId,int bidderId,double bidAmount) {
        // CHÚ Ý: Vì gói tin mạng chạy ở luồng ngầm (Background Thread),
        // bắt buộc phải dùng Platform.runLater để can thiệp vào giao diện (UI Thread) nhằm tránh crash.
        Platform.runLater(() -> {
            // 1. Thêm lượt đặt giá mới vào ĐẦU danh sách (vị trí số 0) 
            // Điều này giúp lượt đặt giá mới nhất luôn nhảy lên dòng ĐẦU TIÊN của bảng để dễ nhìn.
            // TODO: fix
            //bidDataList.add(0,new BidTransaction(auctionId,bidderId,bidAmount));

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
    }
    public void loadInitialData(List<BidTransaction> serverHistory) {
        if (serverHistory == null || serverHistory.isEmpty()) return;

        bidDataList.clear();

        // Kiểm tra tối ưu ngay từ lúc nạp dữ liệu ban đầu
        if (serverHistory.size() > MAX_BIDS_TO_DISPLAY) {
            // Nếu Server trả về quá nhiều (ví dụ 1000 dòng), ta chỉ cắt lấy 20 dòng mới nhất đưa vào UI
            List<BidTransaction> truncatedList = serverHistory.subList(0, MAX_BIDS_TO_DISPLAY);
            bidDataList.addAll(truncatedList);
            for (int i = 0 ; i < truncatedList.size() ; i ++) {
                BidTransaction bid = truncatedList.get(i);
                priceSeries.getData().add(new XYChart.Data<>(bid.getTimeStamp(),bid.getBidAmount()));
            }
            ClientLogger.info("Đã cắt bớt lịch sử đấu giá ban đầu để tối ưu RAM.");
        } else {
            bidDataList.addAll(serverHistory);
            for (int i = 0 ;  i < serverHistory.size(); i ++ ) { 
                BidTransaction bid = serverHistory.get(i);
                priceSeries.getData().add(new XYChart.Data<>(bid.getTimeStamp(),bid.getBidAmount()));
            }
        }
    }

    @Override 
    public void onAuctionCancelled(AuctionCancelledEvent event) {
        Platform.runLater(() -> {
            AlertUtils.showAlert(Alert.AlertType.INFORMATION, "THÔNG BÁO", event.getReason());
            bidButton.setDisable(true);

        });
        
    }
    @Override
    public void onAuctionFinished(AuctionFinisedEvent event) {
        Platform.runLater(() -> {
            AlertUtils.showAlert(Alert.AlertType.INFORMATION, "THÔNG BÁO", "PHIÊN ĐẤU GIÁ ĐÃ ĐƯỢC HOÀN THÀNH");
            bidButton.setDisable(true);

        });
    }
    @Override 
    public void onAuctionStarted(AuctionStartedEvent event) {
        Platform.runLater(() -> {
            AlertUtils.showAlert(Alert.AlertType.INFORMATION,"THÔNG BÁO"," PHIÊN ĐẤU GIÁ BẮT ĐẦU ");
            bidButton.setDisable(false);
        });
    }
    @Override 
    public void onAuctionEnded(AuctionEndedEvent event) {
        Platform.runLater(() -> {
            AlertUtils.showAlert(Alert.AlertType.INFORMATION,"THÔNG BÁO"," PHIÊN ĐẤU GIÁ KẾT THÚC ! CHỜ THANH TOÁN ");
            bidButton.setDisable(true);
        });
    }

}
