package com.groupproject.client;
import com.groupproject.client.network.EventRouter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.SceneNavigator;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.network.CreateAuctionRequest;
import com.groupproject.shared.network.CreateAuctionResponse;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
public class AddItemController  {
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
    @FXML private Label description;
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
    @FXML
    public void initialize() { 
        // tạo thêm một mục là Unknown/Other để id = ? 
        setupTime();
        generateCategory();
        createUI();
        handleImage(); 
        EventRouter.getInstance().on(CreateAuctionResponse.class, this::handleResponseCreateAuction);
    }   
    @FXML
    private void handleCreateAuction(ActionEvent event) throws IOException {
        if(! validInput()) {
            return;
        }
        else {
            setLoadingState(true);
            // Gửi Request tạo ra một phiên đấu giá lên sever để xử lý
            handleRequestCreateAuction();
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

        // Vẫn chỉ là giả định chưa có gì chắc chắn id của nó sẽ là thế này cả !!!!
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
                // ứng với id của other phía trên nhưng có thể sau này sẽ thay đổi nên không có gì cả . Để tạm vậy 
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
                validationLabel.setVisible(false);
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
        if (description.getText().isEmpty() || name.getText()==null) {
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
    // hàm xử lý thời gian giá và cài đặt các giới hạn trong tầm kiểm soát theo nghiệp vụ ( không cho thời gian đấu giá quá lâu hoặc quá nhanh )
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
        try {
            Integer sellerId = SessionManager.getInstance().getCurrentUser().getId();
            String title = name.getText();
            String desc = description.getText();
            Category selectedCategory = category.getValue();
            double startingPrice = Double.parseDouble(startprice.getText());
            LocalDateTime end = LocalDateTime.of(enddate.getValue(), LocalTime.of(endhour.getValue(), endminute.getValue(), endsecond.getValue()));
            DateTimeFormatter formatter= DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String endTime = end.format(formatter);
            CreateAuctionRequest request = new CreateAuctionRequest(sellerId, title, desc, selectedCategory, startingPrice, endTime);
            RequestSender.send(request);
        }
        catch (Exception e) {
            AlertUtils.showError("Error ! ","Can't connect to server");
            e.printStackTrace();
        }
        finally {
            setLoadingState(false);
        }
   }
   private void  handleResponseCreateAuction(CreateAuctionResponse response) {
        if (response.isSuccess()) {
            handleSuccessCreateAuction(response);
        }
        else {
            handleFailCreateAuction(response);
        }
   }     
   private void handleSuccessCreateAuction(CreateAuctionResponse response) {
        AlertUtils.showSuccess("Success !", "Product has been created successfully");
        // Lưu vào đâu  getInstance() của SessionManager().geAllAuctions().
        // Chuyển hướng về Home 
        SceneNavigator.goTo("/com/groupproject/client/FXML/mainscreen.fxml");
   }
   private void handleFailCreateAuction(CreateAuctionResponse response) {
        AlertUtils.showError("Error !", response.getMessage());
   }
}

