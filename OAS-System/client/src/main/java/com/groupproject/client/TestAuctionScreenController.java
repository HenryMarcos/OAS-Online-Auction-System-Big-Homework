package com.groupproject.client;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.transaction.BidDTO;
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
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class TestAuctionScreenController implements Initializable, LifecycleController {
    
    private Auction currentAuction;
    private Timeline timeline;

    @FXML private Label auctionsession;       // The title header
    @FXML private Label auctioncurrentprice;  // The right panel price
    @FXML private Label auctiontimeleft;      // The right panel time
    
    @FXML private TextField enterprice;
    @FXML private Button placebid;

    @FXML private TableView<BidDTO> bottomtable;
    @FXML private TableColumn<BidDTO, String> usercol;
    @FXML private TableColumn<BidDTO, Double> pricecol;
    @FXML private TableColumn<BidDTO, String> timecol;
    
    @FXML private LineChart<String, Number> linechart;
    private ObservableList<BidDTO> bidHistoryList;
    private XYChart.Series<String, Number> bidGraphSeries;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // 1. Setup Table Columns (Matches variable names in BidDTO)
        usercol.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
        pricecol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        timecol.setCellValueFactory(new PropertyValueFactory<>("timeString"));
        
        bidHistoryList = FXCollections.observableArrayList();
        bottomtable.setItems(bidHistoryList);

        // 2. Setup Line Chart
        bidGraphSeries = new XYChart.Series<>();
        bidGraphSeries.setName("Live Price History");
        linechart.getData().add(bidGraphSeries);
        linechart.setAnimated(false); // Prevents graphical glitches when spamming bids

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

    @FXML
    private void handlePlaceBid() {
        try {
            double bidAmount = Double.parseDouble(enterprice.getText());
            
            // Send secure request (Server automatically figures out User ID)
            PlaceBidRequest req = new PlaceBidRequest(currentAuction.getId(), bidAmount);
            RequestSender.send(req);
            
            placebid.setDisable(true); // Prevent spam clicking

        } catch (NumberFormatException e) {
            ClientLogger.error("Please enter a valid number!");
        }
    }

    @FXML
    private void switchtoHome(ActionEvent event) {
        // Because MainController handles the screen swapping, the easiest way 
        // to go back to the home screen from a sub-controller is to use your SceneNavigator 
        // or trigger the main screen reload. 
        // If your SceneNavigator supports it, you can do:
        try {
            // Note: Adjust this depending on how your SceneNavigator works. 
            // If MainController manages the BorderPane, you might need to make 
            // a static MainController.getInstance() to call loadView("homecontent.fxml");
            
            // Example if you reload the whole main screen:
            com.groupproject.client.utils.SceneNavigator.INSTANCE.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
        } catch (Exception e) {
            ClientLogger.error(e.getMessage());
        }
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
                placebid.setDisable(true);
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
            placebid.setDisable(false); // Re-enable button
            
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

            placebid.setDisable(true); 
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

    @Override
    public void cleanup() {}

    // ... rest of your code ...
}