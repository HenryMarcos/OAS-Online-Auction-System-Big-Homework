package com.groupproject.client;
import com.groupproject.client.Data.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.time.LocalDate;

import com.groupproject.client.utils.SceneNavigator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.fxml.Initializable;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
public class AddItemController implements Initializable {
    @FXML 
    private Spinner<Integer> endhour;
    @FXML 
    private Spinner<Integer> endminute;
    @FXML 
    private Spinner<Integer> endsecond;
    @FXML 
    private Label validationLabel;
    @FXML
    ComboBox<Category> category;
    @FXML
    private TextField name ;
    @FXML
    private DatePicker enddate;
    @FXML
    private TextField startprice;
    @FXML
    private Label namelabel;
    @FXML
    private Label datelabel;
    @FXML
    private Label categorylabel;
    @FXML
    private Label pricelabel;
    @FXML
    private Button chooseImageButton;
    @FXML
    private Label imageLabel;
    @FXML
    private File imagefile;
    @FXML 
    private Button addButton;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // them cac category , set hieu ung khi ma mot truong duoc dien 
        // 
        endhour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,23,0));
        endminute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0));
        endsecond.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0));

        enddate.valueProperty().addListener((obs,oldval,newval) -> {
            datelabel.setVisible(newval==null);
        });
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image of Product");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
        chooseImageButton.setOnAction(event -> {
            imagefile = fileChooser.showOpenDialog(null);
            if (imagefile != null) {
                imageLabel.setText(imagefile.getName());
            }
        });
        // kiem tra xem cac truong co duoc dien day du khong 
        addButton.setOnMouseClicked( mouseEvent -> {
            if (name.getText().isEmpty() || startprice.getText().isEmpty() || category.getValue().isEmpty()|| imagefile.getName().isEmpty() || LocalDateTime.now().isAfter(enddate.getValue().atTime(LocalTime.MAX)) ) {
                validationLabel.setVisible(true);
                validationLabel.setText("Please fill all fields correctly");
                return;
            }
            else {
                LocalDate date = enddate.getValue();
                int hour= endhour.getValue();
                int minute= endminute.getValue();
                int second= endsecond.getValue();
                LocalDateTime endDateTime= LocalDateTime.of(date, LocalTime.of(hour, minute, second));
                Item item = new Item(name.getText(),category.getValue(),Double.parseDouble(startprice.getText()),endDateTime,imagefile.toURI().toString());
                ItemRespository.save(item);
                if ( HomeController.getInstance() != null) {
                    HomeController.getInstance().loadItems();
                }
                validationLabel.setVisible(true);
                validationLabel.setStyle("-fx-text-fill:black");
                validationLabel.setText("Add item successfully ! Please go back to HOME");
            }      
        });
    }   
    @FXML
    private void switchtoHome(ActionEvent event) throws IOException {
      SceneNavigator.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
    } 
    @FXML
    private void switchtologin(ActionEvent event) throws IOException {
       SceneNavigator.goTo("/com/groupproject/client/FXML/login.fxml");

    }

    
}

