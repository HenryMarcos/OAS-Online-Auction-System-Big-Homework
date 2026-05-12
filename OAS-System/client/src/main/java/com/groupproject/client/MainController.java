package com.groupproject.client;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.stage.Stage;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.client.utils.SceneNavigator;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;

public class MainController extends Application  implements Initializable {
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private VBox profilesubmenu;
    @FXML 
    private Button profilebtn;
    @FXML
    private Label username;
    // test chay ham home
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
       SceneNavigator.goTo("/com/groupproject/client/FXML/homecontent.fxml");
       
   } 
   @FXML
   private void switchtoAddItem() throws IOException {
      SceneNavigator.goTo("/com/groupproject/client/FXML/additem.fxml");
   }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SceneNavigator.goTo("/com/groupproject/client/FXML/homecontent.fxml");
        // yêu cầu nhả ra các categories đã có sẵn trong máy.
         // Thiết lập tên dựa vào username
         String name= SessionManager.getInstance().getCurrentUser().getUsername();
         username.setText(name);

         // lắng nghe gọi GetCategoriesResponse 


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
        SceneNavigator.goTo("/com/groupproject/client/FXML/notification.fxml");
        // ĐĂNG KÝ NGHE ĐỂ TRẢ VỀ THÔNG BÁO 
    }
    public static void main(String[] args) {
        launch(args);
    }

}
