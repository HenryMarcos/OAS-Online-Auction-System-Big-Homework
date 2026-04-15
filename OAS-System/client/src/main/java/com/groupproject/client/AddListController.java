package com.groupproject.client;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.control.DatePicker;

public class AddListController implements Initializable {
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
}

