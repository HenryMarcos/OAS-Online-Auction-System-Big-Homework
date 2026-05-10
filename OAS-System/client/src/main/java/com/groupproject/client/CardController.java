package com.groupproject.client;

import javafx.scene.Parent;
import java.io.IOException;

import javafx.scene.image.ImageView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.client.utils.CountDownHelper;

public class CardController { 
    private Auction auction;
    private CountDownHelper countDownHelper = new CountDownHelper();
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
        auctioncontroller.setAuction(auction, currentprice, timeleft, productname);
        Scene scene = new Scene(root,1000,700);
        scene.getStylesheets().add(getClass().getResource("/com/groupproject/client/CSS/auctionscreen.css").toExternalForm());
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Auction Screen | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(scene);
        currentStage.show();
    }
    public void setAuction(Auction auction) {
        this.auction = auction;
        productname.setText("Name: " + auction.getItemName());
        currentprice.setText("Current price: " + auction.getCurrentBid());
        
        // Sử dụng Helper để tránh viết lại logic đếm ngược
        countDownHelper.start(auction, () -> timeleft.setText("ENDED"), timeleft);
    }
}
