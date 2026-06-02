package com.groupproject.client;
// giao dien, logic cua trang chu

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.GetAuctionRequest;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;




// phan center cua mainscreen.fxml 
public class HomeController extends BaseAuctionViewController {
    private static HomeController instance;

    @FXML private GridPane productgrid;
    @FXML private Button sortbutton;
    // Khi nhan vao nut Log out o mep ben phai cua man hinh 

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        addEventHandles();

        // Tải các phiên đấu giá đang hoạt động lên màn hình mỗi khi mở lên
        loadAuctions();
      
    }

    public void addEventHandles()  {
        sortbutton.setOnMouseClicked(mouseEvent -> {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("/com/groupproject/client/FXML/sortmenu.fxml"));
            try {
                AnchorPane root = loader.load();
                stage.setScene(new Scene(root));
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // Tải các phiên đấu giá
    // ---------------------
    public void loadAuctions() {
        // Xóa các data cũ đi
        productgrid.getChildren().clear();
 
        // Lấy danh sách các phiên đấu giá đang hoạt động 
        List<Auction> liveAuctions = SessionManager.INSTANCE.getCurrentAuctionList();
 
        // Kiểm tra xem danh sách có trống không
        if (liveAuctions == null || liveAuctions.isEmpty()) {
            ClientLogger.warning("No active auctions to display.");
            return;
        }
        // Tạo 1 card mới từ mỗi auction
        for (int i = 0; i < liveAuctions.size(); i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/groupproject/client/FXML/card.fxml"));
                HBox card = loader.load();
                
                // Configure layout sizes
                card.setMaxWidth(Double.MAX_VALUE);
                GridPane.setFillWidth(card, true);
                
                // Pass the Auction object to the card
                CardController controller = loader.getController();
                controller.setAuction(liveAuctions.get(i));
            
                // Add to grid (i % 2 gives column 0 or 1, i / 2 gives row 0, 1, 2...)
                productgrid.add(card, i % 2, i / 2);

            } catch (IOException e) {
                System.err.println("Failed to load card.fxml for an auction!");
                e.printStackTrace();
            }
        }
    }  

    // hàm load những items có trong từng mục category
    @Override
    public boolean shouldInclude(Auction newItem) {
        return true;
    }

    @Override
    public void fetchInitialData() {
        GetAuctionRequest request = GetAuctionRequest.getAll();
        RequestSender.send(request);
    }

    @Override
    public void fetchDataByCategory(int categoryId) {
        GetAuctionRequest request = GetAuctionRequest.getByStatus(null, categoryId);
        RequestSender.send(request);
    }
}
