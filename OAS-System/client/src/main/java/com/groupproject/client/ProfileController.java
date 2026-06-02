package com.groupproject.client;

import com.groupproject.client.network.ClientMessageRouter;
import com.groupproject.client.network.RequestSender;
import com.groupproject.client.utils.AlertUtils;
import com.groupproject.client.utils.SessionManager;
import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.requests.TopUpRequest;
import com.groupproject.shared.network.responses.TopUpResponse;

import javafx.application.Platform;
import javafx.fxml.FXML;
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

    // TopUp controls
    @FXML private TextField topUpAmountField;
    @FXML private Button topUpButton;

    public static ProfileController getInstance() { return instance; }

    @FXML
    public void initialize() {
        loadUserData();

        // Lắng nghe phản hồi TopUp từ server
        ClientMessageRouter.INSTANCE.onResponse(TopUpResponse.class, this::handleTopUpResponse);
    }

    private void loadUserData() {
        Platform.runLater(() -> {
            User user = SessionManager.INSTANCE.getCurrentUser();
            if (user == null) return;
            userIdField.setText(String.valueOf(user.getId()));
            usernameField.setText(user.getUsername());
            emailField.setText(user.getEmail());
            // Cập nhật trực tiếp trên FX thread
            double balance = user.getBalance();
            availableBalance = balance;
            wallet.setText(String.format("Wallet : %,.0f USD", balance));
        });
    }

    public void updateWallet(double balance) {
        this.availableBalance = balance;
        // Cập nhật label ở trang Profile (nếu đang hiển thị)
        if (wallet != null) {
            wallet.setText(String.format("Wallet : %,.0f USD", balance));
        }
        // Cập nhật label ở sidebar MainController
        MainController main = SessionManager.INSTANCE.getCurrentMainController();
        if (main != null) {
            main.updateWallet(balance);
        }
    }

    // --- Xử lý nút Nạp tiền ---
    @FXML
    private void handleTopUp() {
        if (topUpAmountField == null) return;
        String input = topUpAmountField.getText().trim();

        if (input.isEmpty()) {
            AlertUtils.showError("Lỗi", "Vui lòng nhập số tiền muốn nạp.");
            return;
        }

        try {
            double amount = Double.parseDouble(input);
            if (amount <= 0) {
                AlertUtils.showError("Lỗi", "Số tiền phải lớn hơn 0.");
                return;
            }
            topUpButton.setDisable(true);
            topUpButton.setText("Đang xử lý...");
            RequestSender.send(new TopUpRequest(amount));
        } catch (NumberFormatException e) {
            AlertUtils.showError("Lỗi định dạng", "Vui lòng chỉ nhập số.");
        }
    }

    // --- Nhận phản hồi từ server ---
    private void handleTopUpResponse(TopUpResponse response) {
        Platform.runLater(() -> {
            topUpButton.setDisable(false);
            topUpButton.setText("Nạp tiền");

            if (response.isSuccess()) {
                topUpAmountField.clear();
                updateWallet(response.getNewBalance());
                // Cập nhật balance trong session (nếu User có setter)
                User user = SessionManager.INSTANCE.getCurrentUser();
                if (user != null) {
                    user.setBalance(response.getNewBalance());
                }
                AlertUtils.showSuccess("Nạp tiền thành công",
                        String.format("Số dư mới: %,.0f USD", response.getNewBalance()));
            } else {
                AlertUtils.showError("Nạp tiền thất bại", response.getMessage());
            }
        });
    }

    @FXML
    private void handleSave() {
        // Placeholder — chưa implement
    }
}
