package com.groupproject.client;
import javafx.fxml.FXML;

import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.user.User;

import javafx.scene.control.Alert.AlertType;
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
        // có một số cái phải được gọi xuống từ database: userid, username,emailfield và những cái này là không sửa được
        User user = SessionManager.getInstance().getCurrentUser();
        userIdField.setText(String.valueOf(user.getId()));
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        // Ghi chú: userID, username, email đã được set editable="false" bên FXML
    }
    // Xử lý sau khi có thời gian ( chưa cần thiết ngay lúc này )
    // Xử lý sự kiện khi nhấn nút Save
    @FXML
    private void handleSave() {
        // Lấy dữ liệu mới từ các trường cho phép sửa
        String updatedBank = bankAccountField.getText();
        String updatedAddress = addressField.getText();
        String content = "Thông tin của bạn đã được cập nhật thành công!\n\n" 
                + "Bank Account mới: " + updatedBank + "\n"
                + "Địa chỉ mới: " + updatedAddress;
        // gửi 
        // TODO: Gọi logic lưu vào Database hoặc API ở đây
        // boolean isSaved = database.updateProfile(updatedBank, updatedAddress);

        // Hiển thị thông báo thành công cho người dùng
        AlertUtils.showAlert(AlertType.INFORMATION, "Lưu thông tin",content);

    }

}

