package com.groupproject.client;
import com.groupproject.client.Data.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.time.LocalDate;
import java.util.List;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.network.GetCategoryFieldRequest;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
public class AddItemController implements Initializable {
    @FXML private VBox dynamicFieldsContainer;
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
    private Button chooseImageButton;
    @FXML
    private Label imageLabel;
    @FXML
    private File imagefile;
    @FXML 
    private Button addButton;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) { 
        // tạo thêm một mục là Unknown/Other để id = ? 
        generateCategory();
        createUI();
        handleImage();
        // kiem tra xem cac truong co duoc dien day du khong 
        addButton.setOnMouseClicked( mouseEvent -> {
            if (name.getText().isEmpty() || startprice.getText().isEmpty() || imagefile.getName().isEmpty() || LocalDateTime.now().isAfter(enddate.getValue().atTime(LocalTime.MAX)) ) {
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
                if ( HomeController.getInstance() != null) {
                    HomeController.getInstance().loadItems();
                }
                validationLabel.setVisible(true);
                validationLabel.setStyle("-fx-text-fill:black");
                validationLabel.setText("Auction created successfully ! Please go back to HOME ");
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
    private void handleImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image of Product");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
        chooseImageButton.setOnAction(event -> {
            imagefile = fileChooser.showOpenDialog(null);
            if (imagefile != null) {
                imageLabel.setText(imagefile.getName());
            }
        });
    }
    // ham xu ly thoi gian 
    private boolean handleTime() {
        endhour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,23,0));
        endminute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0));
        endsecond.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0));
        return true;
    }
    // Khi category được bấm vào thì hiện ra màn hình chỉ là tên của category đó mà không phải là địa chỉ
    private void generateCategory() {
        List<Category> dbCategories = SessionManager.getInstance().getCurrentCategories();
        category.getItems().addAll(dbCategories);
        // Giả định đặt ( chưa phải chính thức)
        category.getItems().add(new Category(0, "Other", null));
        category.setConverter(new StringConverter<Category>() {
            @Override public String toString(Category c) {return c == null ? "": c.getName();} 
            @Override public Category fromString(String s) {return null;}
        }); 
    }
    // Tạo ra thêm UI của từng trường thông tin mới khi người dùng chọn vào một category bất kỳ 
    private void createUI() {
        category.getSelectionModel().selectedItemProperty().addListener((obs,newval,oldval)-> {
            if (newval != null) {
                dynamicFieldsContainer.getChildren().clear();
                if (newval.getId()==0) {
                    System.out.println("Khong can phai load them truong moi");
                }
                else {
                   List<String> myFields= newval.getRequiredFields();
                   if (myFields != null && !myFields.isEmpty()) {
                        generateDynamicFields(myFields);
                   }
                }
            }
        });
    }
    // tạo ra label, textfield với từng trường ở trong mỗi category
    private void generateDynamicFields(List<String> fields) {
        for (String fieldName: fields) {
            Label label = new Label(fieldName);
            label.setStyle("-fx-font-size: 14px");
            TextField text = new TextField();
            text.setPromptText("Enter " + fieldName);
            dynamicFieldsContainer.getChildren().addAll(label,text);
        }
    }
    private void setLoadingState(boolean isLoading) {
        addButton.setDisable(isLoading);
        if (isLoading) {
            validationLabel.setVisible(isLoading);
            validationLabel.setText("Processing... Please wait for minutes !");
        }
        else {

        }
    }
    private boolean validInput() {
        if (name.getText()==null || name.getText().isEmpty()) {
            showError();
            return false;
        }
        if( category.getValue() == null) {
            showError();
            return false;
        }
        
    }
    
}

