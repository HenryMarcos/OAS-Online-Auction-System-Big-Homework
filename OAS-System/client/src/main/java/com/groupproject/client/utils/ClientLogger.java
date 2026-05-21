package com.groupproject.client.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientLogger {
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String LOG_FILE = "client_debug.log";
    private static PrintWriter fileWriter;

    // TODO: Thêm tính năng viết log vào file .txt nếu cần

    // Static block để khởi tạo File Writer 1 lần duy nhất khi app bật
    static {
        try {
            // "true" có nghĩa là Append Mode: ghi tiếp vào file cũ thay vì xóa đi tạo lại
            fileWriter = new PrintWriter(new FileWriter(LOG_FILE, true));
            fileWriter.println("\n=======================================================");
            fileWriter.println("APP STARTED AT " + LocalDateTime.now().format(timeFormat));
            fileWriter.println("=======================================================");
            fileWriter.flush();
        } catch (IOException e) {
            System.err.println("CRITICAL: Could not create log file!");
        }
    }

    // In thông báo bình thường
    public static void info(String message) {
        printLog("INFO", message, "\u001B[34m"); // Màu xanh dương cho INFO
    }

    // In thông báo cảnh báo (Màu vàng)
    public static void warning(String message) {
        printLog("WARN", message, "\u001B[33m"); 
    }

    // In thông báo lỗi
    public static void error(String message) {
        printLog("ERROR", message, "\u001B[31m"); // Màu đỏ cho ERROR
    }

    public static void printLog(String level, String message, String colorCode) {
        // Lấy thông tin về nơi gọi hàm này
        // Index 0: getStackTrace()
        // Index 1: printLog()
        // Index 2: info() hoặc error()
        // Index 3: Class thực sự đã gọi lệnh log!
        StackTraceElement caller = Thread.currentThread().getStackTrace()[3];

        String fullClassName = caller.getClassName(); // Lấy tên class(có cả package)
        // Loại bỏ package cho tiện theo dõi
        String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1); 

        // Lấy tên hàm
        String methodName = caller.getMethodName();

        // Lấy thời gian hiện tại
        String timestamp = LocalDateTime.now().format(timeFormat);

        // TẠO CHUỖI GỐC (KHÔNG MÀU) DÀNH CHO FILE
        String cleanFileLine = String.format("[%s] [%s] [%s::%s] %s", 
            timestamp, level, simpleClassName, methodName, message);

        // Định dạng màu sắc (Reset color ở cuối)
        String resetColor = "\u001B[0m";

        // BỌC MÀU VÀO CHUỖI ĐỂ DÀNH CHO CONSOLE
        String coloredConsoleLine = colorCode + cleanFileLine + resetColor;        

        System.out.printf(coloredConsoleLine + "\n");

        // VD: [14:30:05] [INFO] [ClientHandler::run] Client connected.
        if (fileWriter != null) {
            fileWriter.println(cleanFileLine);
            fileWriter.flush(); // QUAN TRỌNG: Bắt buộc lưu ngay lập tức xuống ổ cứng
        }
    }
}
