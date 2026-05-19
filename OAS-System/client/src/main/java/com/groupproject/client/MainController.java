package com.groupproject.client;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.stage.Stage;

import com.groupproject.client.utils.SessionManager;
import com.groupproject.client.utils.NotificationStore;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.shared.network.Notification;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;

public class MainController extends Application  implements Initializable {
    private static final MainController instance = new MainController();
    private double availableBalance;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private VBox profilesubmenu;
    @FXML 
    private Button profilebtn;
    @FXML
    private Label username;
    @FXML private Label wallet;
    @FXML private Label redDotIndicator;
    // test chay ham home
    public static MainController getInstance() {
        return instance;
    }
    @Override
    public void start(Stage primarystage) throws IOException {
        SceneNavigator.setMainStage(primarystage);
        SceneNavigator.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
    }
    @FXML 
    private void switchtologin(ActionEvent event) throws IOException {
        SceneNavigator.goTo("/com/groupproject/client/FXML/login.fxml");
        // set User == null;
      
   }
   // Khi nhan vao nut Home o man hinh chinh 
   @FXML
   private void switchtoHome() throws IOException {
       loadViewIntoCenter("/com/groupproject/client/FXML/homecontent.fxml");
       
   } 
   @FXML
   private void switchtoAddItem() throws IOException {
      loadViewIntoCenter("/com/groupproject/client/FXML/additem.fxml");
   }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadViewIntoCenter("/com/groupproject/client/FXML/homecontent.fxml");
        // yêu cầu nhả ra các categories đã có sẵn trong máy.
         // Thiết lập tên dựa vào username
        updateUI();
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
    @FXML
    private void switchtoNotification() {
        NotificationStore.getInstance().markAllReads();
        loadViewIntoCenter("/com/groupproject/client/FXML/notification.fxml");
        // ĐĂNG KÝ NGHE ĐỂ TRẢ VỀ THÔNG BÁO 
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
    private void updateUI() {
        Platform.runLater(() -> {
            String name= SessionManager.getInstance().getCurrentUser().getUsername();
            username.setText(name);
            updateWallet(availableBalance);
    
        });
    }
    public void updateWallet(double availableBalance) {
        this.availableBalance= availableBalance;
        wallet.setText(String.format("Wallet : %,.0f USD",availableBalance));
    }
    public static void main(String[] args) {
        launch(args);
    }
    private void loadViewIntoCenter(String fxmlPath) {
        try {
            // 1. Tải giao diện của màn hình con
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            
            // 2. Đặt giao diện vừa tải vào phần CENTER của BorderPane
            mainBorderPane.setCenter(view);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Lỗi: Không thể tải giao diện " + fxmlPath);
        }
    }

}
