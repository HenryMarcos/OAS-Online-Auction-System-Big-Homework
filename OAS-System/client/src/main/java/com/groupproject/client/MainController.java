package com.groupproject.client;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
public class MainController  implements Initializable {
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private VBox profilesubmenu;
    @FXML 
    private Button profilebtn;
    @FXML 
    private void switchtologin(ActionEvent event) throws IOException {
        
      Parent root = FXMLLoader.load(getClass().getResource("FXML/login.fxml"));
        
      Scene scene = new Scene(root, 1000, 700);
      Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
      currentStage.setTitle("Login | Auction System");
      // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
      currentStage.setScene(scene);
      currentStage.show();
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
         loadView("homecontent.fxml");
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

}
