package com.groupproject.client;

import java.net.URL;
import java.util.ResourceBundle;

import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class TestAuctionScreenController implements Initializable, LifecycleController {
    
    private Auction currentAuction;

    @FXML private Label auctionsession;       // The title header
    @FXML private Label auctioncurrentprice;  // The right panel price
    @FXML private Label auctiontimeleft;      // The right panel time
    
    @FXML private TextField enterprice;
    @FXML private Button placebid;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Retrieve the auction we just joined from the SessionManager
        this.currentAuction = SessionManager.INSTANCE.getCurrentViewingAuction();
        
        if (this.currentAuction != null) {
            setupUI();
        }

        // Add your NewBidEvent live-update listeners here...
    }

    private void setupUI() {
        auctionsession.setText(currentAuction.getTitle());
        
        double displayPrice = (currentAuction.getCurrentBid() > 0) ? currentAuction.getCurrentBid() : currentAuction.getStartingPrice();
        auctioncurrentprice.setText("Price: $" + displayPrice);
        
        // TODO: Start a new Timeline here to update `auctiontimeleft` just like in the CardController!
        //auctionproductname.setText(currentAuction.getTitle());
        //auctioncurrentprice.setText("$" + currentAuction.getCurrentBid());
        // Setup countdown timer...
    }

    @FXML
    private void handlePlaceBid() {
        // Handle sending the bid...
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

    @Override
    public void cleanup() {}

    // ... rest of your code ...
}