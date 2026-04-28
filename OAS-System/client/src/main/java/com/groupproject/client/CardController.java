package com.groupproject.client;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
public class CardController {
    @FXML
    private Label productname;
    @FXML
    private Label currentprice;
    @FXML
    private Label timeleft;
    @FXML 
    private void handleBid(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("FXML/auctionscreen.fxml"));
        Scene scene = new Scene(root,1000,700);
        scene.getStylesheets().add(getClass().getResource("/com/groupproject/client/CSS/auctionscreen.css").toExternalForm());
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Auction Screen | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(scene);
        currentStage.show();
    }
    
}
