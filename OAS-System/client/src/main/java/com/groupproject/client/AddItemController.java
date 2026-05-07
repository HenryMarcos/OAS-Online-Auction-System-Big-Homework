package com.groupproject.client;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;
import java.time.temporal.ChronoUnit;
import com.groupproject.client.Utlis.Item;
import com.groupproject.client.Utlis.ItemRespository;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
// xử lý thêm trường hợp khách hàng bấm nút add item nhiều lần cùng một lúc 
public class AddItemController implements Initializable {
    @FXML private Spinner<Integer> endhour;
    @FXML private Spinner<Integer> endminute;
    @FXML private Spinner<Integer> endsecond;
    @FXML private Label validationLabel;
    @FXML ComboBox<String> category;
    @FXML private TextField name ;
    @FXML private DatePicker enddate;
    @FXML private TextField startprice;
    @FXML private TextField description;
    @FXML private Button chooseImageButton;
    @FXML private Label imageLabel;
    @FXML private File imagefile;
    @FXML private Button addButton;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // them cac category , set hieu ung khi ma mot truong duoc dien 
        category.getItems().addAll("Art","Clothes","Electronics","Jewelry","Vehicle","Others");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image of Product");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
        chooseImageButton.setOnAction(event -> {
            imagefile = fileChooser.showOpenDialog(null);
            if (imagefile != null) {
                imageLabel.setText(imagefile.getName());
            }
        });
        endhour.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,23,0));
        endminute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0));
        endsecond.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,59,0));

        // kiem tra xem cac truong co duoc dien day du khong 
        addButton.setOnMouseClicked( mouseEvent -> {
            if (name.getText().isEmpty()|| description.getText().isEmpty()|| category.getValue().isEmpty()|| !handlePrice() || imagefile.getName().isEmpty() || !handleTime() ) {
                validationLabel.setVisible(true);
                validationLabel.setText("Please fill all fields correctly");
            }
            // xử lý phần thời gian- double check tu sever.  
            else {
                LocalDate date = enddate.getValue();
                int hour= endhour.getValue();
                int minute= endminute.getValue();
                int second= endsecond.getValue();
                LocalDateTime endDateTime= LocalDateTime.of(date, LocalTime.of(hour, minute, second));
                /* 
                phần này sẽ bao gồm xử lý khi có nhiều lần bấm cùng một lúc 

                addButton.setDisable(true);
                addButton.setText("Processing...");
                */
                Item item = new Item(name.getText(),category.getValue(),Double.parseDouble(startprice.getText()),endDateTime,description.getText(),imagefile.toURI().toString());
                ItemRespository.save(item);
                if ( HomeController.getInstance() != null) {
                    HomeController.getInstance().loadItems();
                }
                validationLabel.setVisible(true);
                validationLabel.setStyle("-fx-text-fill:black");
                validationLabel.setText("Product created successfully ! Please go back to HOME");
            }
                
        }); 
    } 
    // hàm xử lý thời gian.
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
    // xu ly gia
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
}

