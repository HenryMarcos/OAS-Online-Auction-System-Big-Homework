package com.groupproject.server.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

public class ImageStorageManager {
    // Define where to save images on the server machine
    private static final String UPLOAD_DIR = "server_data/auction_images/";

    static {
        new File(UPLOAD_DIR).mkdirs(); // Create folder if it doesn't exist
    }

    // Saves the bytes to disk and returns the generated filename
    public static String saveImage(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) return null;

        // Generate a random unique filename
        String fileName = UUID.randomUUID().toString() + ".jpg";
        String filePath = UPLOAD_DIR + fileName;

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(imageBytes);
            return fileName; // Return just the filename to save in DB
        } catch (Exception e) {
            ServerLogger.error("Failed to save image: " + e.getMessage());
            return null;
        }
    }
}
