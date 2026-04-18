package com.groupproject.client;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javafx.scene.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.control.DatePicker;

public class AddItemController implements Initializable {
    @FXML
    private TextField description;
    @FXML
    ComboBox category;
    @FXML
    private TextField name ;
    @FXML
    private DatePicker date;
    @FXML
    private TextField price;
    @FXML
    private Label namelabel;
    @FXML
    private Label datelabel;
    @FXML
    private Label categorylabel;
    @FXML
    private Label pricelabel;
    @FXML
    private Label descriptionlabel;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        category.getItems().addAll("Art","Clothes","Electronics","Jewelry","Vehicle","Others");
        setupTextFieldAnimation(name,namelabel);
        setupTextFieldAnimation(price,pricelabel);
        setupTextFieldAnimation(description,descriptionlabel);
        setupComboBoxAnimation(category, categorylabel);
        setupDatePickerAnimation(date,datelabel);

    }   
    private void setupTextFieldAnimation(TextField field, Label label) {
            field.focusedProperty().addListener((obs,oldval,newval) -> {
                boolean shouldmoveup = (newval || !field.getText().isEmpty());
                animateLabel(label,shouldmoveup);
            });
    }
    private void setupComboBoxAnimation(ComboBox<String> category, Label categorylabel) {
            category.focusedProperty().addListener((obs,oldval,newval) -> {
                boolean shouldmoveup = (newval || category.getValue() != null);
                animateLabel(categorylabel,shouldmoveup);
            });
    }
    private void setupDatePickerAnimation(DatePicker date, Label datelabel) {
            date.focusedProperty().addListener((obs,oldval,newval) -> {
                boolean shouldmoveup = ( newval || date.getValue() != null) ;
                animateLabel(datelabel,shouldmoveup);
            });
    }

    private void animateLabel (Label label,boolean shouldmoveup) {
        if (shouldmoveup) {
            label.setTranslateY(-35); // Bay lên 35px
            label.setScaleX(0.85);    // Thu nhỏ lại 85%
            label.setScaleY(0.85);
            label.setTextFill(Color.web("#000000"));
        }
        else {
            label.setTranslateY(0);
            label.setScaleX(1.0);
            label.setScaleY(1.0);
            label.setTextFill(Color.web("#999999"));
        }
    }
    @FXML
    private void switchtoHome(ActionEvent event) throws IOException {
      Parent root = FXMLLoader.load(getClass().getResource("/com/groupproject/client/FXML/home.fxml"));
    
        // Bước 2: Tạo một Scene (Cảnh diễn) mới từ giao diện vừa tải
        Scene newScene = new Scene(root,1000,700);
        newScene.getStylesheets().add(getClass().getResource("CSS/home.css").toExternalForm());
        // Bước 3: Lấy lại Sân khấu (Stage) hiện tại từ nút bấm mà người dùng vừa click
        Stage currentStage =  (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.setTitle("Home | Auction System");
        // Bước 4: Kéo rèm! Gắn Cảnh mới lên Sân khấu và hiển thị
        currentStage.setScene(newScene);
        currentStage.show();
   } 
    
}

