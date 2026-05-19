package com.groupproject.client;
import javafx.fxml.FXML;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.user.User;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
public class ProfileController {
    private static final ProfileController instance = new ProfileController();
    private double availableBalance;
    @FXML private ImageView avatarImageView;
    @FXML private TextField userIdField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField bankAccountField;
    @FXML private TextField addressField;
    @FXML private Label ratingLabel;
    @FXML private Button saveButton;
    @FXML private Label wallet;

    public static ProfileController getInstance() {
        return instance;
    }
    @FXML
    public void initialize() {
        // Mockup dữ liệu ban đầu (Trong thực tế bạn sẽ lấy từ Database/Model)
        loadUserData();
    }

    private void loadUserData() {
        Platform.runLater(() -> {
            User user = SessionManager.getInstance().getCurrentUser();
            userIdField.setText(String.valueOf(user.getId()));
            usernameField.setText(user.getUsername());
            emailField.setText(user.getEmail());
            updateWallet(availableBalance);
            
        });
        // có một số cái phải được gọi xuống từ database: userid, username,emailfield và những cái này là không sửa được
        
        // Ghi chú: userID, username, email đã được set editable="false" bên FXML
    }
    public void updateWallet(double availableBalance) {
        this.availableBalance=availableBalance;
        wallet.setText(String.format("Wallet : %,.0f USD",availableBalance));
    }
    // Xử lý sau khi có thời gian ( chưa cần thiết ngay lúc này )
    // Xử lý sự kiện khi nhấn nút Save
    

}

