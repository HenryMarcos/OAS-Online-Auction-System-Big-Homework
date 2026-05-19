package com.groupproject.server.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {
    
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    // In thông báo bình thường
    public static void info(String message) {
        printLog("INFO", message, "\u001B[34m"); // Màu xanh dương cho INFO
    }

    // In thông báo cảnh báo
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

        // Định dạng màu sắc (Reset color ở cuối)
        String resetColor = "\u001B[0m";

        // VD: [14:30:05] [INFO] [ClientHandler::run] Client connected.
        System.out.printf("%s[%s] [%s] [%s::%s] %s%n", 
            colorCode, timestamp, level, simpleClassName, methodName, message + resetColor);
    }

}
