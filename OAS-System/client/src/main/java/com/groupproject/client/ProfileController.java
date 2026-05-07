package com.groupproject.client;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
public class ProfileController {
    @FXML private ImageView avatarImageView;
    @FXML private TextField userIdField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField bankAccountField;
    @FXML private TextField addressField;
    @FXML private Label ratingLabel;
    @FXML private Button saveButton;
    @FXML
    public void initialize() {
        // Mockup dữ liệu ban đầu (Trong thực tế bạn sẽ lấy từ Database/Model)
        loadUserData();
    }

    private void loadUserData() {
        // có một số cái phải được gọi xuống từ sever: userid, username,emailfield và những cái này là không sửa được 
        userIdField.setText("USR-109283");
        usernameField.setText("Cuong_Auction_Pro");
        emailField.setText("cuong.auction@example.com");
        // Ghi chú: userID, username, email đã được set editable="false" bên FXML
    }

    // Xử lý sự kiện khi nhấn nút Save
    @FXML
    private void handleSave() {
        // Lấy dữ liệu mới từ các trường cho phép sửa
        String updatedBank = bankAccountField.getText();
        String updatedAddress = addressField.getText();
        // gửi 
        // TODO: Gọi logic lưu vào Database hoặc API ở đây
        // boolean isSaved = database.updateProfile(updatedBank, updatedAddress);

        // Hiển thị thông báo thành công cho người dùng
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Lưu thông tin");
        alert.setHeaderText(null);
        alert.setContentText("Thông tin của bạn đã được cập nhật thành công!\n\n" 
                + "Bank Account mới: " + updatedBank + "\n"
                + "Địa chỉ mới: " + updatedAddress);
        alert.showAndWait();
    }

}
