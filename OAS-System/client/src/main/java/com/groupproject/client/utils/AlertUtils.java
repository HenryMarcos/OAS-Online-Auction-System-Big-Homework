package com.groupproject.client.utils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class AlertUtils {

    /**
     * Hiển thị thông báo LỖI (Icon dấu X đỏ)
     */
    public static void showError(String title, String message) {
        showAlert(AlertType.ERROR, title, message);
    }

    /**
     * Hiển thị thông báo THÀNH CÔNG/THÔNG TIN (Icon chữ I xanh)
     */
    public static void showSuccess(String title, String message) {
        showAlert(AlertType.INFORMATION, title, message);
    }

    /**
     * Hiển thị CẢNH BÁO (Icon dấu chấm than vàng)
     */
    public static void showWarning(String title, String message) {
        showAlert(AlertType.WARNING, title, message);
    }

    /**
     * Hàm dùng chung để tạo và hiển thị Alert.
     * Đã được bọc trong Platform.runLater để đảm bảo an toàn khi gọi từ luồng Server trả về.
     */
    private static void showAlert(AlertType type, String title, String message) {
        // LUÔN LUÔN đảm bảo UI cập nhật trên JavaFX Thread
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null); // Set null để ẩn phần Header rườm rà
            alert.setContentText(message);
            
            // showAndWait() sẽ làm ứng dụng dừng lại chờ người dùng bấm OK
            alert.showAndWait(); 
        });
    }
}