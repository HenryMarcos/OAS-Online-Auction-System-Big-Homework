package com.groupproject.server.dao;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseManagerTest {

    @BeforeAll
    static void setUpBeforeClass() {
        try {
            DatabaseManager dbManager = DatabaseManager.INSTANCE;
            
            Field field = DatabaseManager.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            
            HikariConfig hikariConfig = new HikariConfig();
            
            // SỬA DÒNG NÀY: Dùng shared memory để tất cả các connection trong pool xài chung 1 DB
            hikariConfig.setJdbcUrl("jdbc:sqlite:file:testdb?mode=memory&cache=shared"); 
            
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(5000);
            
            // Bỏ dòng WAL mode đi vì In-memory của SQLite không hỗ trợ WAL tốt khi dùng Shared Cache
            // hikariConfig.addDataSourceProperty("journal_mode", "WAL"); 
            
            HikariDataSource realDataSource = new HikariDataSource(hikariConfig);
            field.set(dbManager, realDataSource);
            
        } catch (Exception e) {
            fail("Không thể thiết lập lại DatabaseManager thật bằng Reflection: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test khởi tạo Singleton và lấy Connection")
    void testGetConnection() {
        DatabaseManager dbManager = DatabaseManager.INSTANCE;
        assertNotNull(dbManager, "Instance của DatabaseManager không được null");

        Connection conn = dbManager.getConnection();
        assertNotNull(conn, "Connection lấy từ pool không được null");
        
        try {
            assertFalse(conn.isClosed(), "Connection phải đang ở trạng thái mở (open)");
            // Trả connection về pool
            conn.close(); 
        } catch (Exception e) {
            fail("Bắn ra ngoại lệ khi kiểm tra connection: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test khởi tạo Database schema và nạp Seed data")
    void testInitDatabase() {
        DatabaseManager dbManager = DatabaseManager.INSTANCE;
        
        // Chạy hàm khởi tạo database thật trên RAM và đảm bảo không văng lỗi
        assertDoesNotThrow(() -> dbManager.initDatabse(), "Hàm initDatabse() không được ném ra Exception");

        // Kiểm tra xem database thật đã tạo thành công các bảng và dữ liệu mẫu chưa
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
             
            assertNotNull(stmt, "Statement không được null khi kết nối database thật hoạt động");

            // Kiểm tra bảng users đã có tài khoản admin (id = 999999) chưa
            try (ResultSet rs = stmt.executeQuery("SELECT username, email FROM users WHERE id = 999999")) {
                assertTrue(rs.next(), "Phải tồn tại user admin mẫu trong database");
                assertEquals("admin", rs.getString("username"), "Username phải là 'admin'");
                assertEquals("admin@test.com", rs.getString("email"), "Email phải khớp với seed data");
            }

            // Kiểm tra bảng categories xem đã nạp dữ liệu cây danh mục hàng chưa
            try (ResultSet rsCategories = stmt.executeQuery("SELECT count(*) AS total FROM categories")) {
                assertTrue(rsCategories.next());
                assertTrue(rsCategories.getInt("total") > 0, "Bảng categories phải được mẫu hóa dữ liệu");
            }
            
        } catch (Exception e) {
            fail("Không thể truy vấn database sau khi init: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test shutdown Connection Pool")
    void testShutdown() {
        DatabaseManager dbManager = DatabaseManager.INSTANCE;
        
        // Gọi đóng pool thật
        assertDoesNotThrow(() -> dbManager.shutdown(), "Shutdown không được ném ra Exception");
        
        // Sau khi đóng một Pool thật, hàm getConnection() của bạn (ở code gốc) 
        // sẽ bắt lỗi SQLException và trả về null một cách chính xác.
        Connection conn = dbManager.getConnection();
        assertNull(conn, "Connection phải trả về null sau khi pool đã bị đóng hoàn toàn");
    }
}