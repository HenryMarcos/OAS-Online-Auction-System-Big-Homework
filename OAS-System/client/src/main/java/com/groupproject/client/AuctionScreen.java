package com.groupproject.client;
import java.io.IOException;

import javafx.scene.Parent;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
public class AuctionScreen extends Application {
    @Override
    public void start (Stage primarystage) throws IOException {
        primarystage.setTitle("Auction Online Screen");
        Parent root = FXMLLoader.load(getClass().getResource("/com/groupproject/client/FXML/auctionscreen.fxml"));
        Scene scene = new Scene(root,1000,700);
        scene.getStylesheets().add(getClass().getResource("/com/groupproject/client/CSS/auctionscreen.css").toExternalForm());
        primarystage.setScene(scene);
        primarystage.show();
    }
}
