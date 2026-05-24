package com.groupproject.server.utils;

import java.io.ObjectOutputStream;

import com.groupproject.shared.model.user.User; // Import thêm User

public class ClientContext {
    /**
     * ThreadLocal đảm bảo rằng mỗi ClientThread sẽ có một bản sao 'out' độc lập.
     */
    public static final ThreadLocal<ObjectOutputStream> currentOut = new ThreadLocal<>();
    
    // THÊM: Lưu trữ thông tin User đang gắn với luồng (kết nối) này
    public static final ThreadLocal<User> currentUser = new ThreadLocal<>();
    
    // Hàm tiện ích để dọn dẹp khi kết thúc (tránh rò rỉ bộ nhớ luồng)
    public static void clear() {
        currentOut.remove();
        currentUser.remove(); // Nhớ xóa luôn cả user khi client ngắt kết nối
    }
}