package com.groupproject.client;
import com.groupproject.client.Data.*;
import com.groupproject.client.network.EventRouter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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
        setupTime();
        generateCategory();
        createUI();
        handleImage(); 
    }   
    @FXML
    private void handleCreateAuction(ActionEvent event) throws IOException {
        if(! validInput()) {
        }
        else {
            setLoadingState(true);
            // Gửi Request tạo ra một phiên đấu giá lên sever để xử lý

        }
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
    private void setupTime() {
        endhour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,23,0));
        endminute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0));
        endsecond.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0));
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
        if (!handleTime()) {
            showError();
            return false;
        }
        if (!handlePrice()) {
            showError();
            return false ;
        }
        return true;
        // phần xử lý hình ảnh và cả các trường mới được tạo ra khi chọn category của từng loại 
        
    }
    private void showError() {
        validationLabel.setVisible(true);
        validationLabel.setText("Please fill all fields correctly");
    }
    // hàm xử lý thời gian giá 
    public boolean handleTime() {
        LocalDate selectedDate = enddate.getValue();
        if (selectedDate == null) {
            return false;
        }
        int hour= endhour.getValue();
        int minute = endminute.getValue();
        int second= endsecond.getValue();
        // Kiem tra hop le cua ngay thang nam 
        if ( hour <0 || hour > 23 || second < 0 || second  > 59 || minute <0 || minute > 59 ) {
            return false;
        }
        LocalTime selectedTime = LocalTime.of(hour, minute, second);
        LocalDateTime endDateTime = LocalDateTime.of(selectedDate, selectedTime);
        LocalDateTime currentDateTime = LocalDateTime.now();
        if (endDateTime.isBefore(currentDateTime)) {
            return false;
        }
        long daysbetween = ChronoUnit.DAYS.between(currentDateTime, endDateTime);
        long minutesbetween = ChronoUnit.MINUTES.between(currentDateTime,endDateTime);
            if (minutesbetween < 5 || daysbetween > 30 ) {
                return false ;
            }
        return true;
    }
    // Hàm xử lý giá 
    public boolean handlePrice() {
        String selectedPrice= startprice.getText().trim();
        if (selectedPrice == null) {
            return false;
        }
        try {
            double price = Double.parseDouble(selectedPrice);
            if (price <=  0 ) {
                return false ;
            }
            return true;
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            return false ;
        }
    }
    private void handleRequestCreateAuction() {
      /*
         Tạo ra để lăng nghe kết quả trả về từ Sever
       */
      // EventRouter.getInstance().on()
   }     
    
}

