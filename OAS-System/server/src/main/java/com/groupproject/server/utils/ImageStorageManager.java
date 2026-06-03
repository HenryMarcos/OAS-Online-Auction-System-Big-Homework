package com.groupproject.server.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;

public class ImageStorageManager {
    // Thư mục lưu trữ ảnh trên ổ cứng của Server
    private static final String UPLOAD_DIR = "server_data/auction_images/";

    static {
        new File(UPLOAD_DIR).mkdirs(); // Tự động tạo thư mục nếu chưa có
    }

    // 🌟 FIX: Đã thêm tham số String fileName để khớp với CreateAuctionHandler
    public static String saveImage(byte[] imageBytes, String fileName) {
        if (imageBytes == null || imageBytes.length == 0) return null;

        // Đảm bảo đuôi file là .jpg
        if (!fileName.endsWith(".jpg")) {
            fileName += ".jpg";
        }
        
        String filePath = UPLOAD_DIR + fileName;

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(imageBytes);
            return fileName; // Chỉ trả về tên file để lưu vào Database
        } catch (Exception e) {
            ServerLogger.error("Failed to save image: " + e.getMessage());
            return null;
        }
    }

    // 🌟 NEW: Hàm này dùng để đọc ảnh từ ổ cứng lên lại thành byte[] để gửi cho Client
    public static byte[] loadImage(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        try {
            File file = new File(UPLOAD_DIR + fileName);
            if (file.exists()) {
                return Files.readAllBytes(file.toPath());
            }
        } catch (Exception e) {
            ServerLogger.error("Failed to load image from disk: " + e.getMessage());
        }
        return null; // Trả về null nếu ảnh bị xóa hoặc không tồn tại
    }
}