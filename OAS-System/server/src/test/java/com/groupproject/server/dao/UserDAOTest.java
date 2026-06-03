package com.groupproject.server.dao;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groupproject.shared.model.user.User;
import com.groupproject.shared.network.requests.LoginRequest;
import com.groupproject.shared.network.requests.SignupRequest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {

    @BeforeAll
    static void setUpBeforeClass() {
        try {
            // 1. Dọn dẹp Test Pollution & Thiết lập In-Memory Shared Cache
            DatabaseManager dbManager = DatabaseManager.INSTANCE;
            Field field = DatabaseManager.class.getDeclaredField("dataSource");
            field.setAccessible(true);
            
            HikariConfig hikariConfig = new HikariConfig();
            // Đặt tên DB mới (userdao_test) để không đụng hàng với các class test khác
            hikariConfig.setJdbcUrl("jdbc:sqlite:file:userdao_test?mode=memory&cache=shared"); 
            hikariConfig.setMaximumPoolSize(5);
            
            HikariDataSource testDataSource = new HikariDataSource(hikariConfig);
            field.set(dbManager, testDataSource);
            
            // 2. Khởi tạo schema (bảng users, admin_list,...)
            dbManager.initDatabse();
            
            // 3. Đảm bảo có sẵn 1 user làm admin trong bảng admin_list (giả sử ID = 999999)
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT OR IGNORE INTO users (id, username, email, password, balance, created_at) " +
                             "VALUES (999999, 'admin', 'admin@test.com', 'adminpass', 0, CURRENT_TIMESTAMP)");
                stmt.execute("INSERT OR IGNORE INTO admin_list (user_id) VALUES (999999)");
            }
            
        } catch (Exception e) {
            fail("Setup Database thất bại: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Đăng ký User mới")
    void testRegisterUser() {
        User newUser = UserDAO.registerUser("testuser1", "test1@gmail.com", "password123");
        
        assertNotNull(newUser, "Đăng ký thành công phải trả về object User");
        assertTrue(newUser.getId() > 0, "ID của user phải lớn hơn 0");
        assertEquals("testuser1", newUser.getUsername());
        assertEquals("test1@gmail.com", newUser.getEmail());
        assertEquals(10000.0, newUser.getAccountBalance(), "Số dư mặc định khi tạo tài khoản theo logic là 10000");
    }

    @Test
    @Order(2)
    @DisplayName("Test Kiểm tra trùng lặp (Duplicates)")
    void testCheckDuplicates() {
        // Tạo sẵn 1 user để test trùng
        UserDAO.registerUser("dupUser", "dup@gmail.com", "123");

        // Nhánh 1: Trùng cả username lẫn email
        String case1 = UserDAO.checkDuplicates("dupUser", "dup@gmail.com");
        assertEquals("Username and email already exists.", case1);

        // Nhánh 2: Chỉ trùng Username
        String case2 = UserDAO.checkDuplicates("dupUser", "newemail@gmail.com");
        assertEquals("Username is already exists", case2);

        // Nhánh 3: Chỉ trùng Email
        String case3 = UserDAO.checkDuplicates("newuser2", "dup@gmail.com");
        assertEquals("An account with that email already exists.", case3);

        // Nhánh 4: Không trùng gì cả
        String case4 = UserDAO.checkDuplicates("newuser3", "newemail3@gmail.com");
        assertNull(case4, "Nếu không trùng thì hàm checkDuplicates phải trả về null");
    }

    @Test
    @Order(3)
    @DisplayName("Test hàm kiểm tra trùng lặp bằng object SignupRequest (Nạp chồng)")
    void testCheckDuplicatesWithRequest() {
        SignupRequest mockRequest = mock(SignupRequest.class);
        when(mockRequest.getUsername()).thenReturn("dupUser"); // dupUser đã tồn tại ở test số 2
        when(mockRequest.getEmail()).thenReturn("dup@gmail.com");
        
        String result = UserDAO.checkDuplicates(mockRequest);
        assertEquals("Username and email already exists.", result);
    }

    @Test
    @Order(4)
    @DisplayName("Test Authentication: Đăng nhập và Lấy thông tin User")
    void testCheckAndGetUser() {
        UserDAO.registerUser("loginUser", "login@gmail.com", "securePass");

        // 1. Test đăng nhập đúng thông tin
        assertTrue(UserDAO.checkUser("loginUser", "securePass"), "Mật khẩu đúng phải trả về true");
        
        // 2. Test đăng nhập sai thông tin
        assertFalse(UserDAO.checkUser("loginUser", "wrongPass"), "Mật khẩu sai phải trả về false");
        
        // 3. Test lấy thông tin user qua Username + Password
        User fetchedUser = UserDAO.getUser("loginUser", "securePass");
        assertNotNull(fetchedUser);
        assertEquals("login@gmail.com", fetchedUser.getEmail());

        // 4. Test lấy thông tin user qua ID
        User userById = UserDAO.getUserById(fetchedUser.getId());
        assertNotNull(userById);
        assertEquals("loginUser", userById.getUsername());
    }

    @Test
    @Order(5)
    @DisplayName("Test Authentication bằng object LoginRequest (Nạp chồng)")
    void testCheckAndGetUserWithRequest() {
        LoginRequest mockRequest = mock(LoginRequest.class);
        when(mockRequest.getUsername()).thenReturn("loginUser");
        when(mockRequest.getPassword()).thenReturn("securePass");

        assertTrue(UserDAO.checkUser(mockRequest));
        assertNotNull(UserDAO.getUser(mockRequest));
    }

    @Test
    @Order(6)
    @DisplayName("Test Quản lý số dư (Balance)")
    void testBalanceOperations() {
        User user = UserDAO.registerUser("moneyUser", "money@gmail.com", "123");
        int userId = user.getId();

        // Kiểm tra số dư mặc định ban đầu
        double initialBalance = UserDAO.getBalance(userId);
        assertTrue(initialBalance > 0, "Số dư ban đầu phải lấy được thành công");

        // Cộng thêm tiền
        boolean isAdded = UserDAO.addBalance(userId, 5000.0);
        assertTrue(isAdded, "Cộng tiền phải thành công");
        assertEquals(initialBalance + 5000.0, UserDAO.getBalance(userId));

        // Cập nhật đè (set) số dư mới
        boolean isUpdated = UserDAO.updateBalance(userId, 777.0);
        assertTrue(isUpdated, "Cập nhật lại số dư phải thành công");
        assertEquals(777.0, UserDAO.getBalance(userId));
    }

    @Test
    @Order(7)
    @DisplayName("Test kiểm tra quyền Admin")
    void testIsAdmin() {
        // ID 999999 đã được insert ở Setup
        assertTrue(UserDAO.isAdmin(999999), "User ID 999999 phải là Admin");

        // Tạo một user bình thường mới tinh
        User normalUser = UserDAO.registerUser("normalUser", "normal@gmail.com", "123");
        assertFalse(UserDAO.isAdmin(normalUser.getId()), "User mới tạo không thể là Admin");
    }
}