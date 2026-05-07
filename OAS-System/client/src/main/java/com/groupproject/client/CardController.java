package com.groupproject.client;
import javafx.scene.Parent;
import java.io.IOException;

import com.groupproject.client.Utlis.*;

import javafx.beans.binding.Bindings;
import javafx.scene.image.ImageView;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
public class CardController { 
    private ItemViewModel viewModel;
    private CountdownHelper countdownHelper = new CountdownHelper();
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
        auctioncontroller.setItemViewModel(this.viewModel);
        Scene scene = new Scene(root,1000,700);
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Auction Screen | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(scene);
        currentStage.show();
    }
    public void setItem(Item item) {
        this.viewModel = new ItemViewModel(item);
        productname.setText("Name: " + item.getName());
        currentprice.textProperty().bind(Bindings.format("Current price : %,.2f USD", viewModel.currentPriceProperty()));
        countdownHelper.start(item, timeleft);
    }
    
}
