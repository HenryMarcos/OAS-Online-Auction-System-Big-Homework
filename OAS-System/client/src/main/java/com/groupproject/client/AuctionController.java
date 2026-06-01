package com.groupproject.client;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.groupproject.client.network.AuctionIntegrationService;
import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.transaction.BidDTO;
import com.groupproject.shared.network.AuctionEvent.AuctionCancelledEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionFinisedEvent;
import com.groupproject.shared.network.AuctionEvent.AuctionListener;
import com.groupproject.shared.network.AuctionEvent.AuctionStartedEvent;
import com.groupproject.shared.network.events.AuctionEndedEvent;
import com.groupproject.shared.network.events.NewBidEvent;
import com.groupproject.shared.network.requests.PlaceBidRequest;
import com.groupproject.shared.network.responses.PlaceBidResponse;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
// MAX_BIDS_TO_DISPLAY: có thể thêm nếu cần thiết bởi không cần thiết phải lấy hết tất cả mà chỉ cần lấy 10 người đứng đầu thôi chẳng hạn.
public class AuctionController implements AuctionListener  {
    private Auction currentAuction;
    private Timeline timeline;

    private final ObservableList<BidDTO> bidDataList= FXCollections.observableArrayList();
    private AuctionIntegrationService integrationService;
    private final int MAX_BIDS_TO_DISPLAY=20;

    @FXML private Label auctionsession;       // The title header
    @FXML private Label auctioncurrentprice;  // The right panel price
    @FXML private Label auctiontimeleft;      // The right panel time

    @FXML private TextField enterprice;
    @FXML private Button bidButton;

    @FXML private TableView<BidDTO> bottomtable;
    @FXML private TableColumn<BidDTO, String> usercol;
    @FXML private TableColumn<BidDTO, Double> pricecol;
    @FXML private TableColumn<BidDTO, String> timecol;
    
    @FXML private LineChart<String, Number> linechart;
    private ObservableList<BidDTO> bidHistoryList;
    private XYChart.Series<String, Number> bidGraphSeries;
    
    @FXML private Label startprice;
    @FXML private ImageView productImageView;
    @FXML private Label participant;
    @FXML private Label auctionproductname;

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
        currentAuction = SessionManager.INSTANCE.getCurrentViewingAuction();
        int id = currentAuction.getId();
        this.integrationService = new AuctionIntegrationService(id, this);
        this.integrationService.startListening();
        
        // Cài đặt bảng 
        setUpTableView();
        // Linechart
        setupLineChart();

        // 3. Load basic auction data
        // Retrieve the auction we just joined from the SessionManager
        this.currentAuction = SessionManager.INSTANCE.getCurrentViewingAuction();
        
        if (this.currentAuction != null) {
            setupUI();
        }

        // 2. REGISTER NETWORK LISTENERS
        ClientMessageRouter.INSTANCE.onEvent(NewBidEvent.class, this::handleNewBidEvent);
        ClientMessageRouter.INSTANCE.onEvent(AuctionEndedEvent.class, this::handleAuctionEndedEvent);
        ClientMessageRouter.INSTANCE.onResponse(PlaceBidResponse.class, this::handlePlaceBidResponse);
    }

    private void setUpTableView() {
        // 1. Setup Table Columns (Matches variable names in BidDTO)
        usercol.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
        pricecol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        timecol.setCellValueFactory(new PropertyValueFactory<>("timeString"));
        
        bidHistoryList = FXCollections.observableArrayList();
        bottomtable.setItems(bidHistoryList);
    }

    private void setupLineChart() {
        bidGraphSeries = new XYChart.Series<>();
        bidGraphSeries.setName("Live Price History");
        linechart.getData().add(bidGraphSeries);
        linechart.setAnimated(false); // Prevents graphical glitches when spamming bid
    }

    private void setupUI() {
        auctionsession.setText(currentAuction.getTitle());
        
        double displayPrice = (currentAuction.getCurrentBid() > 0) ? currentAuction.getCurrentBid() : currentAuction.getStartingPrice();
        auctioncurrentprice.setText("Price: $" + displayPrice);        

        // Setup timer
        if (currentAuction.getEndTime() != null) {
            timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
                updateCountDown();
            }));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            updateCountDown(); // Run once immediately

            // Plot the starting price on the graph at time 0
            bidGraphSeries.getData().add(new XYChart.Data<>("Start", currentAuction.getStartingPrice()));
            
            // 4. LOAD HISTORICAL BIDS
            List<BidDTO> pastBids = SessionManager.INSTANCE.getCurrentAuctionBids();
            if (pastBids != null) {
                for (BidDTO pastBid : pastBids) {
                    // index 0 means it dynamically pushes the newest rows to the TOP of the table!
                    bidHistoryList.add(0, pastBid); 
                    
                    // Add sequentially to graph so it draws left-to-right
                    bidGraphSeries.getData().add(new XYChart.Data<>(pastBid.getTimeString(), pastBid.getAmount()));
                }
            }
            
            // Set final displayed price
            displayPrice = currentAuction.getCurrentBid() > 0 ? currentAuction.getCurrentBid() : currentAuction.getStartingPrice();
            updatePriceDisplay(displayPrice);

        } else {
            auctiontimeleft.setText("No End Time");
        }

    }
    /* 
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
    */

    @FXML
    private void handlePlaceBid() {
        String priceText = enterprice.getText().trim();
        if (priceText.isEmpty()) {
            AlertUtils.showError("Lỗi nhập liệu ! ","Vui lòng nhập số tiền mà bạn muốn");
            return;
        }
        try {
            double bidAmount = Double.parseDouble(priceText);

            bidButton.setDisable(true); // Safeguard UI spamming
            if (bidAmount <= 0) {
                AlertUtils.showError("Lỗi logic","Số tiền đấu giá phải lớn hơn 0");
                bidButton.setDisable(false);
                return;
            }
            // Kiểm tra xem giá vừa nhập đang cao hơn giá hiện tại không ? 
            if (bidAmount <= currentAuction.getCurrentBid() ) {
                AlertUtils.showError("Lỗi logic","Giá đặt phải cao hơn giá hiện tại");
                bidButton.setDisable(false);
                return;
            }

            PlaceBidRequest request = new PlaceBidRequest(currentAuction.getId(), bidAmount);
            RequestSender.send(request);
            enterprice.clear();
            bidButton.setDisable(true);
            bidButton.setText("Loading...");
            
        }
        catch (NumberFormatException e ) {
            AlertUtils.showError("Lỗi định dạng", "Vui lòng chỉ nhập số");
            ClientLogger.error("Invalid Price Input Format");
        }
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

    // --- REAL-TIME EVENT UPDATER ---
    private void handleNewBidEvent(NewBidEvent event) {
        // Double check if the incoming bid is for the auction we are currently looking at
        if (currentAuction != null && currentAuction.getId() == event.getAuctionId()) {
            Platform.runLater(() -> {
                // Update underlying data object
                currentAuction.setCurrentBid(event.getNewBidAmount());
                currentAuction.setHighestBidderId(event.getHighestBidderId());
                
                // Instantly update the visual UI label 
                updatePriceDisplay(event.getNewBidAmount());
                
                // Make the text pop visually so the user knows it changed!
                auctioncurrentprice.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                
                // Optional: reset the style back to normal after 1 second
                new Thread(() -> {
                    try { Thread.sleep(1000); } catch (Exception e) {}
                    Platform.runLater(() -> auctioncurrentprice.setStyle("-fx-text-fill: #000000; -fx-font-weight: normal;"));
                }).start();

                // 2. CREATE BID ENTRY FOR TABLE & GRAPH
                String bidderStr = "User " + event.getHighestBidderId();
                LocalDateTime now = LocalDateTime.now();
                BidDTO newBid = new BidDTO(bidderStr, event.getNewBidAmount(), now);
                
                // Add to table (index 0 puts it at the very top of the table)
                bidHistoryList.add(0, newBid); 
                
                // Add to graph
                bidGraphSeries.getData().add(new XYChart.Data<>(newBid.getTimeString(), newBid.getAmount()));
            });
        }
    }

    private void handleAuctionEndedEvent(AuctionEndedEvent event) {
        // Ensure the event is for the auction we are currently viewing
        if (currentAuction != null && currentAuction.getId() == event.getAuctionId()) {
            Platform.runLater(() -> {
                // 1. Force the timer to stop
                if (timeline != null) timeline.stop();
                
                auctiontimeleft.setText("OFFICIALLY CLOSED");
                auctiontimeleft.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

                // 2. Lock the User Interface to prevent further bidding
                bidButton.setDisable(true);
                enterprice.setDisable(true);
                enterprice.setPromptText("Auction Ended");

                // 3. Display the Winner
                if (event.getWinnerId() != null && event.getWinnerId() > 0) {
                    auctioncurrentprice.setText(String.format("Winner: User %d ($%.2f)", event.getWinnerId(), event.getWinningBidAmount()));
                    auctioncurrentprice.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;"); // Orange text for winner
                } else {
                    auctioncurrentprice.setText("Ended with no bids.");
                    auctioncurrentprice.setStyle("-fx-text-fill: #7f8c8d;"); // Gray text for no winner
                }
            });
        }
    }

    // --- RESPONSE HANDLER ---
    private void handlePlaceBidResponse(PlaceBidResponse response) {
        Platform.runLater(() -> {
            bidButton.setDisable(false); // Re-enable button
            
            if (response.isSuccess()) {
                enterprice.clear();
            } else {
                ClientLogger.warning("Bid failed: " + response.getMessage());
            }
        });
    }

    private void updatePriceDisplay(double price) {
        ClientLogger.info("Updating price display");
        auctioncurrentprice.setText(String.format("$%.2f", price));
    }

    public void updateCountDown() {
        if (currentAuction.getEndTime() == null) return;

        LocalDateTime syncedNow = com.groupproject.client.utils.TimeUtil.getNow();

        Duration remaining = Duration.between(syncedNow, currentAuction.getEndTime());
        
        if (remaining.isNegative() || remaining.isZero()) {
            auctiontimeleft.setText("ENDED");

            bidButton.setDisable(true); 
            enterprice.setDisable(true);

            if (auctiontimeleft != null) timeline.stop();
        } else {
            long totalSeconds = ChronoUnit.SECONDS.between(syncedNow, currentAuction.getEndTime());
            long days    = totalSeconds / 86400;           
            long hours   = (totalSeconds % 86400) / 3600;  
            long minutes = (totalSeconds % 3600) / 60;     
            long seconds = totalSeconds % 60;              

            auctiontimeleft.setText(String.format("Ending in: %dd : %02dh : %02dm : %02ds", days, hours, minutes, seconds));
        }
    }

}
