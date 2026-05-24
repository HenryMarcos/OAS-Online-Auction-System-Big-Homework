package com.groupproject.server.utils;

import java.io.ObjectOutputStream;

public class ClientContext {
    /**
     * ThreadLocal đảm bảo rằng mỗi ClientThread sẽ có một bản sao 'out' độc lập.
     * Các luồng không bao giờ dẫm chân lên dữ liệu của nhau.
     */
    public static final ThreadLocal<ObjectOutputStream> currentOut = new ThreadLocal<>();
    
    // Hàm tiện ích để dọn dẹp khi kết thúc (tránh rò rỉ bộ nhớ luồng)
    public static void clear() {
        currentOut.remove();
    }
}