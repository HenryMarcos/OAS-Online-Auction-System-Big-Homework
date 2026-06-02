package com.groupproject.client;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.ClientLogger;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.requests.GetAuctionRequest;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class MyAuctionsController extends BaseAuctionViewController {
    @FXML private GridPane productgrid;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        addEventHandles();

        // Tải các phiên đấu giá đang hoạt động lên màn hình mỗi khi mở lên
        loadAuctions();
    }

    private void loadAuctions() {
      // Xóa các data cũ đi
        productgrid.getChildren().clear();
 
        // Lấy danh sách các phiên đấu giá 
        List<Auction> myAuctions = SessionManager.INSTANCE.getMyProductList();
 
        // Kiểm tra xem danh sách có trống không
        if (myAuctions == null || myAuctions.isEmpty()) {
            ClientLogger.warning("No active auctions to display.");
            return;
        }
        // Tạo 1 card mới từ mỗi auction
        for (int i = 0; i < myAuctions.size(); i++) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/groupproject/client/FXML/testCard.fxml"));
                HBox card = loader.load();
                
                // Configure layout sizes
                card.setMaxWidth(Double.MAX_VALUE);
                GridPane.setFillWidth(card, true);
                
                // Pass the Auction object to the card
                TestCardController controller = loader.getController();
                controller.setAuction(myAuctions.get(i));
            
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
        return newItem.getSellerId()==SessionManager.INSTANCE.getCurrentUser().getId().intValue();
    }
    @Override
    public void fetchInitialData() {
        User user = SessionManager.INSTANCE.getCurrentUser();
        int idUser = user.getId().intValue();
        GetAuctionRequest request = GetAuctionRequest.getBySeller(idUser, null);
        RequestSender.send(request);
    }
    @Override 
    public void fetchDataByCategory(int categoryId) {
        User user = SessionManager.INSTANCE.getCurrentUser();
        int idUser = user.getId().intValue();
        GetAuctionRequest request = GetAuctionRequest.getBySeller(idUser, categoryId);
        RequestSender.send(request);
    }

    /**
     * Override để dùng myAuctionCard.fxml riêng thay vì card.fxml chung.
     * Card này có nút "Bắt đầu ngay" và "Hủy phiên".
     */
    @Override
    public Node createCardNode(Auction auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/groupproject/client/FXML/myAuctionCard.fxml")
            );
            Node node = loader.load();
            MyAuctionCardController ctrl = loader.getController();
            ctrl.setAuction(auction);
            registerChildController(ctrl);
            return node;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
