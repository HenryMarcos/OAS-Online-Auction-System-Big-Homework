package com.groupproject.client;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

import com.groupproject.client.network.AuctionEventBus;
import com.groupproject.client.network.AuctionListener;
import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.client.utils.TimeUtil;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.transaction.AuctionDetail;
import com.groupproject.shared.model.transaction.BidDTO;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.events.AuctionEndedEvent;
import com.groupproject.shared.network.events.AuctionFinishedEvent;
import com.groupproject.shared.network.events.AuctionStartedEvent;
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
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
// MAX_BIDS_TO_DISPLAY: có thể thêm nếu cần thiết bởi không cần thiết phải lấy hết tất cả mà chỉ cần lấy 10 người đứng đầu thôi chẳng hạn.
public class AuctionController implements Initializable, LifecycleController,  AuctionListener  {
    private Auction currentAuction;
    private List<BidDTO> bidHistory;
    private Timeline timeline;
    private final int MAX_BIDS_TO_DISPLAY=20;

    @FXML private Label auctionsession;       // The title header
    @FXML private Label auctiondescription;
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

    @FXML private ImageView mainImageView;
    @FXML private HBox subImagesContainer;

    @FXML 
    private void switchtoHome(ActionEvent event) throws IOException {
        SceneNavigator.INSTANCE.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Cài đặt bảng lịch sử đấu giá và linchart giá tiền 
        setUpTableView();
        setupLineChart();

        // Lấy data từ SessionManager
        setupAuctionFromSession();
        
        // Start live countdown timer updates if timeline layout exists
        setupCountdownTimeline();

        // 2. REGISTER NETWORK LISTENERS
        ClientMessageRouter.INSTANCE.onEvent(NewBidEvent.class, this::handleNewBidEvent);
        ClientMessageRouter.INSTANCE.onEvent(AuctionEndedEvent.class, this::handleAuctionEndedEvent);
        ClientMessageRouter.INSTANCE.onResponse(PlaceBidResponse.class, this::handlePlaceBidResponse);
    }

    private void setupAuctionFromSession() {
        AuctionDetail detail = SessionManager.INSTANCE.getCurrentAuctionDetail();
        
        if (detail == null) {
            ClientLogger.warning("Failed to populate UI: No target AuctionDetail payload bound inside SessionManager context.");
            AlertUtils.showError("Error", "Could not load auction view details.");
            return;
        }

        this.currentAuction = detail.getAuction();

        // 1. Map Texts
        if (auctionsession != null) { auctionsession.setText(currentAuction.getTitle()); }
        if (auctiondescription != null) { auctiondescription.setText(detail.getDescription()); }

        // 2. Map Images (Main & Sub Images)
        loadImages(detail);

        // 3. Normalize starting vs current bidding price layout configuration
        double displayPrice = (currentAuction.getCurrentBid() > 0) 
                ? currentAuction.getCurrentBid() 
                : currentAuction.getStartingPrice();
        updatePriceDisplay(displayPrice);

        // 4. Setup Bid History (Table & Graph)
        populateBidHistoryUI(detail.getBidHistory());

        // 5. Setup Countdown Timer
        setupCountdownTimeline();

        // 6. Security Check: Disable bidding if the user is the seller
        User currentUser = SessionManager.INSTANCE.getCurrentUser();
        if (currentUser != null && currentUser.getId() == currentAuction.getSellerId()) {
            bidButton.setDisable(true);
            enterprice.setDisable(true);
            enterprice.setPromptText("Cannot bid on your own item");
        }

        // 7. Subscribe to real-time events via EventBus
        AuctionEventBus.getInstance().subscribe(currentAuction.getId(), this);
    }

    // Tải và hiển thị các hình ảnh chính và phụ
    // -----------------------------------------
    private void loadImages(AuctionDetail detail) {
        // Load Main Image
        if (mainImageView != null && currentAuction.getMainImageBytes() != null) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(currentAuction.getMainImageBytes());
                mainImageView.setImage(new Image(bis));
            } catch (Exception e) {
                ClientLogger.error("Failed to load main image: " + e.getMessage());
            }
        }

        // Load Sub Images if container exists
        if (subImagesContainer != null && detail.getSubImageBytesList() != null) {
            subImagesContainer.getChildren().clear();
            for (byte[] imgBytes : detail.getSubImageBytesList()) {
                try {
                    ImageView subView = new ImageView(new Image(new ByteArrayInputStream(imgBytes)));
                    subView.setFitHeight(80);
                    subView.setFitWidth(80);
                    subView.setPreserveRatio(true);
                    subImagesContainer.getChildren().add(subView);
                } catch (Exception e) {
                    ClientLogger.error("Failed to load a sub image.");
                }
            }
        }
    }

    // Lấy lịch sử bid và thêm vào bảng 
    // --------------------------------
    private void populateBidHistoryUI(List<BidDTO> pastBids) {
        // Plot the starting price at the beginning of the graph
        bidGraphSeries.getData().add(new XYChart.Data<>("Start", currentAuction.getStartingPrice()));

        if (pastBids != null && !pastBids.isEmpty()) {
            // pastBids comes from DB as [Newest, Older, Oldest]
            // We loop backwards to add them as [Oldest, Older, Newest]
            for (int i = pastBids.size() - 1; i >= 0; i--) {
                BidDTO bid = pastBids.get(i);
                // Add to the bottom of the table
                bidHistoryList.add(bid);
            }
            for (int i = 0; i < pastBids.size(); i++) {
                BidDTO bid = pastBids.get(i);
                // Add to the graph (Left to Right)
                bidGraphSeries.getData().add(new XYChart.Data<>(bid.getTimeString(), bid.getAmount()));
            }
        }
    }

    // Cài đặt bảng hiển thị các lượt bid từ trước
    // -------------------------------------------
    private void setUpTableView() {
        usercol.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
        pricecol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        timecol.setCellValueFactory(new PropertyValueFactory<>("timeString"));
        
        bidHistoryList = FXCollections.observableArrayList();
        bottomtable.setItems(bidHistoryList);
    }

    // Cài đặt biểu đồ giá
    // -------------------
    private void setupLineChart() {
        bidGraphSeries = new XYChart.Series<>();
        bidGraphSeries.setName("Live Price History");
        linechart.getData().add(bidGraphSeries);
        linechart.setAnimated(false); // Prevents graphical glitches when spamming bid
    }

    // Hàm tính và hiển thị thời gian còn lại
    // --------------------------------------
    private void setupCountdownTimeline() {
        if (currentAuction.getEndTime() != null) {
            timeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> updateCountDown()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
            updateCountDown(); // Run once immediately
        } else {
            auctiontimeleft.setText("No End Time");
        }
    }

    // Hàm hiển thị thời gian còn lại
    // ------------------------------
    public void updateCountDown() {
        if (currentAuction == null) return;
        if (currentAuction.getEndTime() == null) return;

        String timeString = TimeUtil.formatTimeRemaining(currentAuction.getEndTime());
        auctiontimeleft.setText(timeString);
    
        if ("ENDED".equals(timeString)) {
            if (timeline != null) timeline.stop();

            bidButton.setDisable(true); 
            enterprice.setDisable(true);
        }
    }

    // Hàm hiển thị giá tiền cao nhất hiện tại
    // ---------------------------------------
    private void updatePriceDisplay(double price) {
        ClientLogger.info("Updating price display");
        auctioncurrentprice.setText(String.format("$%.2f", price));
    }

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
    
    @Override
    public void onAuctionFinished(AuctionFinishedEvent event) {
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
        
    }

    // --- REAL-TIME EVENT UPDATER ---\

    // Xử lý việc nhận bid mới
    // -----------------------
    private void handleNewBidEvent(NewBidEvent event) {
        // Kiểm tra xem bid mới này có phải bid mà ta đang tham gia không
        if (currentAuction != null && currentAuction.getId() == event.getAuctionId()) {
            Platform.runLater(() -> {
                // Update dữ liệu mới cho phiên đấu giá hiện tại
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

    // Xử lý việc phiên đấu giá kết thúc
    // ---------------------------------
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
    // Xử lý kết quả của việc đặt bid
    // ------------------------------
    private void handlePlaceBidResponse(PlaceBidResponse response) {
        Platform.runLater(() -> {
            bidButton.setDisable(false); // Re-enable button
            bidButton.setText("PLACE BID");

            if (response.isSuccess()) {
                enterprice.clear();
            } else {
                ClientLogger.warning("Bid failed: " + response.getMessage());
            }
        });
    }

    @Override
    public void cleanup() {
        ClientLogger.info("Cleaning up layout contextual event bindings for active Auction pipeline loop.");
        if (timeline != null) { timeline.stop(); }
        // MUST clean up to prevent memory leaks!
        if (currentAuction != null) {
            AuctionEventBus.getInstance().unsubscribe(currentAuction.getId(), this);
        }
    }

}
