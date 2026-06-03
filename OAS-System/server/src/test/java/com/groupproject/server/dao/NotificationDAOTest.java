package com.groupproject.server.dao;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class NotificationDAOTest {

    private static final int TEST_USER_ID = 1;

    @BeforeAll
    static void setUpBeforeClass() {
        try {
            // 1. Khởi tạo DB trên RAM bằng Shared Cache để các test chạy độc lập và cực nhanh
            DatabaseManager dbManager = DatabaseManager.INSTANCE;
            Field field = DatabaseManager.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:sqlite:file:notifdb?mode=memory&cache=shared"); 
            hikariConfig.setMaximumPoolSize(5);
            
            HikariDataSource testDataSource = new HikariDataSource(hikariConfig);
            field.set(dbManager, testDataSource);
            
            // 2. Tạo toàn bộ schema (các bảng) trong DB RAM
            dbManager.initDatabse();
            
            // 3. Insert một User giả lập vì bảng notifications có ràng buộc FOREIGN KEY với bảng users
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                String insertUserSql = "INSERT OR IGNORE INTO users (id, username, email, password, balance, created_at) " +
                                       "VALUES (" + TEST_USER_ID + ", 'notif_user', 'notif@test.com', '12345', 1000, CURRENT_TIMESTAMP)";
                stmt.execute(insertUserSql);
            }
            
        } catch (Exception e) {
            fail("Setup Database thất bại: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test tạo thông báo thành công lưu vào Database")
    void testCreateNotificationSuccess() {
        String testMessage = "Chúc mừng! Bạn đã thắng phiên đấu giá.";

        // 1. Gọi hàm cần test (Đảm bảo không bị văng Exception)
        assertDoesNotThrow(() -> {
            NotificationDAO.createNotification(TEST_USER_ID, testMessage);
        }, "Hàm createNotification không được ném ra lỗi");

        // 2. Trực tiếp chọc vào DB để kiểm tra xem thông báo đã thực sự được lưu chưa
        String checkSql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
             
            pstmt.setInt(1, TEST_USER_ID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                // Kiểm tra có dòng dữ liệu trả về hay không
                assertTrue(rs.next(), "Dữ liệu thông báo phải tồn tại trong database");
                
                // Kiểm tra tính chính xác của dữ liệu được lưu
                assertEquals(testMessage, rs.getString("message"), "Nội dung message phải khớp");
                assertEquals(TEST_USER_ID, rs.getInt("user_id"), "ID người dùng phải khớp");
                assertFalse(rs.getBoolean("is_read"), "Trạng thái is_read mặc định phải là false (0)");
            }
            
        } catch (Exception e) {
            fail("Truy vấn DB để kiểm tra thất bại: " + e.getMessage());
        }
    }
}