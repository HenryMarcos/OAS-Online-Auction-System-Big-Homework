package com.groupproject.client;
import java.io.IOException;
import com.groupproject.client.network.EventRouter;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.util.ResourceBundle;
import java.net.URL;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.client.utils.CountDownHelper;
import com.groupproject.shared.network.GetAuctionDetailRequest;
import com.groupproject.shared.network.GetAuctionDetailResponse;
import javafx.application.Platform;
import javafx.fxml.Initializable;

public class CardController implements Initializable { 
    private Auction currentAuction;
    private CountDownHelper countDownHelper = new CountDownHelper();
    @FXML
    private ImageView image;
    @FXML
    private Label productname;
    @FXML
    private Label currentprice;
    @FXML
    private Label timeleft;
    @FXML private ToggleButton buttonSubscribe;
    @FXML private Label auctionStatus;
    
    @FXML
    public  void initialize(URL location, ResourceBundle resources) {
        // ĐĂN KÝ VIỆC LẮNG NGHE TRẢ VỀ KẾT QUẢ
        EventRouter.getInstance().on(GetAuctionDetailResponse.class, this::handleGetDetailAuction);
    }
    @FXML 
    private void handleBid(ActionEvent event) throws IOException {
        // Chỉ lưu ID vào session, AuctionController sẽ tự fetch từ Server khi initialize
        GetAuctionDetailRequest request = new GetAuctionDetailRequest(currentAuction.getId());
        RequestSender.send(request);
        
    }
    public void populateUI(Auction auction) {
        Platform.runLater(() -> {
            this.currentAuction = auction;
            productname.setText("Name: " + auction.getItemName());
            currentprice.setText("Current price: " + auction.getCurrentBid());
            
            // Sử dụng Helper để tránh viết lại logic đếm ngược
            countDownHelper.start(auction, () -> timeleft.setText("ENDED"), timeleft);
        });
        
    }
    // Hàm xử lý khi có người muốn theo dõi phiên đấu giá này 
    @FXML
    private void handleSubscribe() {
        if( buttonSubscribe.isSelected()) {
            buttonSubscribe.setText("Following");
            // Gọi code gửi lên sever yêu cầu đưa khách hàng này vào danh sách nhận thông báo 
        }
        else {
            buttonSubscribe.setText("Follow now !");
        }
    }
    private void handleGetDetailAuction(GetAuctionDetailResponse response) {
        if (response.isSuccess()) {
            Platform.runLater(() ->{
                SessionManager.getInstance().setCurrentAuction(response.getAuction());
                SceneNavigator.goTo("/com/groupproject/client/FXML/auctionscreen.fxml");
            });
        }
        else {
            Platform.runLater(() -> {
                AlertUtils.showError("Error !" , "Can't enter the auction now ");
            });
        }
    }
}
