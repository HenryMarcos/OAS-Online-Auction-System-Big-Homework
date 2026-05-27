package com.groupproject.client;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.network.events.AuctionListUpdateEvent;
import com.groupproject.shared.network.responses.JoinAuctionResponse;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
public class MainController  implements Initializable {

    private Object currentSubController;

    @FXML private BorderPane mainBorderPane;
    @FXML private VBox profilesubmenu;
    @FXML private Button profilebtn;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // yêu cầu nhả ra các categories đã có sẵn trong máy.
        loadView("homecontent.fxml");
        // lắng nghe gọi GetCategoriesResponse 
        // 1. Listen for Join Responses
        ClientMessageRouter.INSTANCE.onResponse(JoinAuctionResponse.class, this::handleJoinAuctionResponse);
        
        // 2. NEW: Listen for Global Auction List Updates
        ClientMessageRouter.INSTANCE.onEvent(AuctionListUpdateEvent.class, this::handleAuctionListUpdate);
    }

    @FXML
    private void toggleProfilemenu() {
        boolean iscurrentlyvisible= profilesubmenu.isVisible();
        if (iscurrentlyvisible) {
            profilesubmenu.setVisible(!iscurrentlyvisible);
            profilesubmenu.setManaged(!iscurrentlyvisible);
            profilebtn.setText("Profile ▶ ");
        }
        else {
            profilesubmenu.setVisible(!iscurrentlyvisible);
            profilesubmenu.setManaged(!iscurrentlyvisible);
            profilebtn.setText("Profile ▼ ");
        }
        
    }

    private void loadView(String fxmlFileName) {
        try {
            // Cleanup màn hình phụ trước đó trước khi đổi sang màn hình phụ khác
            if (currentSubController instanceof LifecycleController) {
                ((LifecycleController) currentSubController).cleanup();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/groupproject/client/FXML/" + fxmlFileName));
            Node view = loader.load();

            // Lưu controller phụ mới để dùng
            currentSubController = loader.getController();
            
            // Lệnh này sẽ lấy phần ruột (ví dụ cái ScrollPane) đắp vào khoảng trống ở giữa!
            mainBorderPane.setCenter(view);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void switchtoNotification() {
        loadView("notification.fxml");
    }

    @FXML
    private void switchToCreateAuction() {
        loadView("createAuctionTest.fxml");
    }

    @FXML
    private void switchtologin(ActionEvent event) throws IOException {
        SceneNavigator.INSTANCE.goTo("/com/groupproject/client/FXML/login.fxml");
    }

    // Khi nhan vao nut Home o man hinh chinh 
    @FXML
    private void switchtoHome() throws IOException {
        loadView("homecontent.fxml");
    } 
    @FXML
    private void switchtoAddItem() throws IOException {
        loadView("additem.fxml");
    }

    private void handleJoinAuctionResponse(JoinAuctionResponse response) {
        if (response.isSuccess()) {
            // 1. Save the target auction to our session so the next screen can use it
            SessionManager.INSTANCE.setCurrentViewingAuction(response.getAuction());
            
            // 2. Change the UI to the auction screen!
            loadView("testAuctionScreen.fxml");
        } else {
            // Show an error alert (e.g., "Auction ended")
            System.out.println("Failed to join: " + response.getMessage());
        }
    }

    private void handleAuctionListUpdate(AuctionListUpdateEvent event) {
        // 1. ALWAYS update the session data, regardless of what screen the user is on!
        SessionManager.INSTANCE.setCurrentAuctionList(event.getActiveAuctions());

        // 2. Check if the user is currently looking at the Home Screen
        Platform.runLater(() -> {
            if (currentSubController instanceof HomeController) {
                // If they are on the home screen, force the grid to repaint live!
                HomeController home = (HomeController) currentSubController;
                home.loadAuctions();
            }
        });
    }

}
