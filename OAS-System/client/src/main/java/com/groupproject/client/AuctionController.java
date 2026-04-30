package com.groupproject.client;
import java.io.IOException;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
public class AuctionController {
    @FXML
    private Label currentprice;
    @FXML
    private Label participant;
    @FXML
    private Label productname;
    @FXML 
    private void switchtoHome(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/groupproject/client/FXML/home.fxml"));
    
        // Bước 2: Tạo một Scene (Cảnh diễn) mới từ giao diện vừa tải
        Scene newScene = new Scene(root,1000,700);
        newScene.getStylesheets().add(getClass().getResource("CSS/home.css").toExternalForm());
        // Bước 3: Lấy lại Sân khấu (Stage) hiện tại từ nút bấm mà người dùng vừa click
        Stage currentStage =  (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Home | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(newScene);
        currentStage.show();
    }
}
