package com.groupproject.client;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.LifecycleController;
import com.groupproject.client.utils.NotificationStore;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.client.utils.TimeUtil;
import com.groupproject.shared.model.transaction.NotificationDTO;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.events.AuctionListUpdateEvent;
import com.groupproject.shared.network.events.BalanceUpdateEvent;
import com.groupproject.shared.network.requests.LogOutRequest;
import com.groupproject.shared.network.responses.JoinAuctionResponse;
import com.groupproject.shared.network.responses.LogOutResponse;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController extends Application implements Initializable {

    private Object currentSubController;

    @FXML private BorderPane mainBorderPane;
    @FXML private VBox profilesubmenu;
    @FXML private Button profilebtn;

    @FXML private Button bellButton;
    @FXML private VBox inboxPane;
    @FXML private ListView<NotificationDTO> notificationList;
    
    @FXML private Label username;
    @FXML private Label wallet;
    @FXML private Label redDotIndicator;
    
    @Override
    public void start(Stage primarystage) throws IOException {
        SceneNavigator.INSTANCE.setMainStage(primarystage);
        SceneNavigator.INSTANCE.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
    }
    @FXML 
    private void switchtologin(ActionEvent event) throws IOException {
        SceneNavigator.INSTANCE.goTo("/com/groupproject/client/FXML/login.fxml");
        // set User == null;
    } 

    @FXML
    private void switchtoCreateAuction() throws IOException {
        loadViewIntoCenter("/com/groupproject/client/FXML/createauction.fxml");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadView("homecontent.fxml");
        // 1. Listen for Join Responses
        ClientMessageRouter.INSTANCE.onResponse(JoinAuctionResponse.class, this::handleJoinAuctionResponse);
        ClientMessageRouter.INSTANCE.onResponse(LogOutResponse.class, this::handleLogOutResponse);
        // 2. NEW: Listen for Global Auction List Updates
        ClientMessageRouter.INSTANCE.onEvent(AuctionListUpdateEvent.class, this::handleAuctionListUpdate);
        // 3. NEW: Listen for personal Balance Updates (real-time wallet sync)
        ClientMessageRouter.INSTANCE.onEvent(BalanceUpdateEvent.class, event -> {
            Platform.runLater(() -> {
                // Update local session balance
                User currentUser = SessionManager.INSTANCE.getCurrentUser();
                if (currentUser != null) {
                    currentUser.setAccountBalance(event.getNewBalance());
                }
                // Instantly refresh the wallet label in the top bar
                updateWallet(event.getNewBalance());
            });
        });

        
        // Load notifications into the list
        List<NotificationDTO> myAlerts = SessionManager.INSTANCE.getNotificationList();
        if (myAlerts != null && !myAlerts.isEmpty()) {
            notificationList.getItems().addAll(myAlerts);
            
            // Count unread messages to show on the bell!
            long unreadCount = myAlerts.stream().filter(n -> !n.isRead()).count();
            if (unreadCount > 0) {
                bellButton.setText("🔔 Inbox (" + unreadCount + ")");
                bellButton.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        } else {
            bellButton.setText("🔔 Inbox");
        }
        
        // Thiết lập tên dựa vào username
        updateUI();
        // Đăng ký instance này vào SessionManager để các controller khác gọi được updateWallet()
        SessionManager.INSTANCE.setCurrentMainController(this);
        // lắng nghe gọi GetCategoriesResponse 
        redDotIndicator.visibleProperty().bind(NotificationStore.getInstance().unreadCountProperty().greaterThan(0));
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
        NotificationStore.getInstance().markAllReads();
        loadViewIntoCenter("/com/groupproject/client/FXML/notification.fxml");
        // ĐĂNG KÝ NGHE ĐỂ TRẢ VỀ THÔNG BÁO 
    }
    
    @FXML 
    private void switchToMyAuctions() {
        loadViewIntoCenter("/com/groupproject/client/FXML/myauctions.fxml");
    }

    @FXML 
    private void swichtoMyProducts() {
        loadViewIntoCenter("/com/groupproject/client/FXML/yourauctions.fxml");
    }
    @FXML
    private void switchtoActiveListings() {
        loadViewIntoCenter("/com/groupproject/client/FXML/activeauctions.fxml");
    }
    @FXML
    private void switchtoPersonalInfo() {
        loadViewIntoCenter("/com/groupproject/client/FXML/profile.fxml");
    }
    @FXML
    private void switchtoYourListings() {
        
    }

    private void updateUI() {
        Platform.runLater(() -> {
            User user = SessionManager.INSTANCE.getCurrentUser();
            if (user != null) {
                String name = user.getUsername();
                username.setText(name);
                updateWallet(user.getAccountBalance());
            }
        });
    }
    public void updateWallet(double availableBalance) {
        Platform.runLater(() -> wallet.setText(String.format("Wallet : %,.0f USD", availableBalance)));
    }
    
    private void loadViewIntoCenter(String fxmlPath) {
        try {
            if (currentSubController instanceof LifecycleController) {
                ((LifecycleController) currentSubController).cleanup();
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            currentSubController = loader.getController();
            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không thể tải giao diện " + fxmlPath);
        }
    }

    @FXML
    private void switchToCreateAuction() {
        loadView("createAuctionTest.fxml");
    }

    @FXML
    private void logout(ActionEvent event) throws IOException {
        RequestSender.send(new LogOutRequest());
    }

    // Khi nhan vao nut Home o man hinh chinh 
    @FXML
    private void switchtoHome() throws IOException {
        loadView("homecontent.fxml");
    } 
    @FXML
    private void switchtoAddItem() throws IOException {
        loadView("createAuctionTest.fxml");
    }

    @FXML
    private void toggleInbox() {
        // Show or hide the dropdown inbox
        inboxPane.setVisible(!inboxPane.isVisible());
    }

    @FXML
    private void toggleSubMenu() {
        // Toggles the visibility of the profile sub-menu
        boolean isVisible = profilesubmenu.isVisible();
        profilesubmenu.setVisible(!isVisible);
        profilesubmenu.setManaged(!isVisible); // Ensures it doesn't take up blank space when hidden
    }

    private void handleJoinAuctionResponse(JoinAuctionResponse response) {
        Platform.runLater(() -> {
            if (response.isSuccess()) {
                // Save auction AND past bids to memory
                SessionManager.INSTANCE.setCurrentViewingAuction(response.getAuction());
                SessionManager.INSTANCE.setCurrentAuctionBids(response.getPastBids()); 
                
                loadView("testAuctionScreen.fxml");
            } else {
                System.out.println("Failed to join: " + response.getMessage());
            }
        });
    }

    private void handleLogOutResponse(LogOutResponse response) {
        if (response.isSuccess()) {
            SessionManager.INSTANCE.setCurrentUser(null);
            SceneNavigator.INSTANCE.goTo("/com/groupproject/client/FXML/login.fxml");
        }
    }

    private void handleAuctionListUpdate(AuctionListUpdateEvent event) {
        TimeUtil.syncWithServer(event.getServerTime());

        // 1. ALWAYS update the session data, regardless of what screen the user is on!
        SessionManager.INSTANCE.setCurrentAuctionList(event.getActiveAuctions());

        // 2. Check if the user is currently looking at a list Screen
        Platform.runLater(() -> {
            if (currentSubController instanceof BaseAuctionViewController) {
                // If they are on any auction list screen, force it to fetch new data!
                BaseAuctionViewController listScreen = (BaseAuctionViewController) currentSubController;
                listScreen.fetchInitialData();
            }
        });
    }

}
