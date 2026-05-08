package com.groupproject.client;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.stage.Stage;
import com.groupproject.client.utils.SceneNavigator;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
    // test chay ham home
    @Override
    public void start(Stage primarystage) throws IOException {
        SceneNavigator.setMainStage(primarystage);
        SceneNavigator.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
    }
    @FXML 
    private void switchtologin(ActionEvent event) throws IOException {
        SceneNavigator.goTo("/com/groupproject/client/FXML/login.fxml");
      
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/groupproject/client/FXML/" + fxmlFileName));
            Node view = loader.load();
            
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
    public static void main(String[] args) {
        launch(args);
    }

}
