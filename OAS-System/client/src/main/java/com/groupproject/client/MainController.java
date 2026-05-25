package com.groupproject.client;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.network.responses.JoinAuctionResponse;

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

    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private VBox profilesubmenu;
    @FXML 
    private Button profilebtn;
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
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // yêu cầu nhả ra các categories đã có sẵn trong máy.
        loadView("homecontent.fxml");
        // lắng nghe gọi GetCategoriesResponse 
        // Listen for the server confirming we joined an auction successfully
        ClientMessageRouter.INSTANCE.onResponse(JoinAuctionResponse.class, this::handleJoinAuctionResponse);
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

}
