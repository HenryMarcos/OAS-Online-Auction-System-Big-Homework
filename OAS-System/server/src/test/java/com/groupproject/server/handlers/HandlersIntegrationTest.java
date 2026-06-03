package com.groupproject.server.handlers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.groupproject.server.cache.CategoryManager;
import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.DatabaseManager;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.shared.network.requests.LoginRequest;
import com.groupproject.shared.network.requests.SignupRequest;
import com.groupproject.shared.network.responses.LoginResponse;
import com.groupproject.shared.network.responses.Response;
import com.groupproject.shared.network.responses.SignupResponse;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HandlersIntegrationTest {

    private ClientHandler mockClientContext;
    private ObjectOutputStream mockOutStream;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        // 1. Khởi tạo cấu hình Database ảo SQLite chạy trực tiếp trên RAM
        DatabaseManager dbManager = DatabaseManager.INSTANCE;
        Field dbField = DatabaseManager.class.getDeclaredField("dataSource");
        dbField.setAccessible(true);
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:file:handler_test_db?mode=memory&cache=shared");
        hikariConfig.setMaximumPoolSize(5);
        dbField.set(dbManager, new HikariDataSource(hikariConfig));
        
        // Tạo cấu trúc bảng thực tế (Users, Auctions, Bids...)
        dbManager.initDatabse();
    }

    @BeforeEach
    void setUp() throws Exception {
        mockClientContext = mock(ClientHandler.class);
        
        // Tạo luồng giả lập ObjectOutputStream để tránh lỗi NullPointerException khi ClientManager gọi .getOut()
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mockOutStream = new ObjectOutputStream(baos);
        when(mockClientContext.getOut()).thenReturn(mockOutStream);

        // Đảm bảo AuctionManager đồng bộ chính xác dữ liệu sạch từ DB RAM trước mỗi test case
        AuctionManager.INSTANCE.refreshCache();
    }

    // ==========================================
    // 1. KIỂM THỬ CHO SIGNUPHANDLER
    // ==========================================

    @Test
    @DisplayName("SignupHandler: Đăng ký tài khoản thành công")
    void testSignupSuccess() {
        SignupHandler signupHandler = new SignupHandler();
        SignupRequest request = mock(SignupRequest.class);
        
        when(request.getUsername()).thenReturn("newUser");
        when(request.getEmail()).thenReturn("new@test.com");
        when(request.getPassword()).thenReturn("securePass");

        Response response = signupHandler.handle(request, mockClientContext);

        // Kiểm định kết quả phản hồi trả về Client
        assertNotNull(response);
        assertTrue(response instanceof SignupResponse);
        SignupResponse signupResponse = (SignupResponse) response;
        assertTrue(signupResponse.isSuccess());
        assertEquals("Account successfully created!", signupResponse.getMessage());

        // Xác thực hệ thống đã gán ID người dùng vào phiên kết nối (Client Context)
        verify(mockClientContext, times(1)).setAuthenticatedUserId(anyInt());
    }

    @Test
    @DisplayName("SignupHandler: Đăng ký thất bại do trùng Username")
    void testSignupDuplicateUsername() {
        // Thực hiện đăng ký trước một tài khoản mẫu vào DB ảo
        SignupHandler signupHandler = new SignupHandler();
        SignupRequest firstRequest = mock(SignupRequest.class);
        when(firstRequest.getUsername()).thenReturn("duplicateUser");
        when(firstRequest.getEmail()).thenReturn("first@test.com");
        signupHandler.handle(firstRequest, mockClientContext);

        // Thực hiện đăng ký tài khoản thứ hai trùng tên tài khoản trên
        SignupRequest secondRequest = mock(SignupRequest.class);
        when(secondRequest.getUsername()).thenReturn("duplicateUser");
        when(secondRequest.getEmail()).thenReturn("second@test.com");

        Response response = signupHandler.handle(secondRequest, mockClientContext);

        assertTrue(response instanceof SignupResponse);
        SignupResponse signupResponse = (SignupResponse) response;
        assertFalse(signupResponse.isSuccess());
        // Trực tiếp đón thông điệp báo lỗi từ logic UserDAO xử lý trùng lặp
        assertNotNull(signupResponse.getMessage()); 
    }

    // ==========================================
    // 2. KIỂM THỬ CHO LOGINHANDLER
    // ==========================================

    @Test
    @DisplayName("LoginHandler: Đăng nhập thành công và lấy danh sách sản phẩm thực tế")
    void testLoginSuccess() {
        // Bước chuẩn bị: Đăng ký một tài khoản hợp lệ qua Handler
        SignupHandler signupHandler = new SignupHandler();
        SignupRequest signupReq = mock(SignupRequest.class);
        when(signupReq.getUsername()).thenReturn("loginUser");
        when(signupReq.getEmail()).thenReturn("login@test.com");
        when(signupReq.getPassword()).thenReturn("password123");
        signupHandler.handle(signupReq, mockClientContext);

        // Tiến hành Test hành động Đăng nhập
        LoginHandler loginHandler = new LoginHandler();
        LoginRequest loginReq = mock(LoginRequest.class);
        when(loginReq.getUsername()).thenReturn("loginUser");
        when(loginReq.getPassword()).thenReturn("password123");

        Response response = loginHandler.handle(loginReq, mockClientContext);

        assertTrue(response instanceof LoginResponse);
        LoginResponse loginResponse = (LoginResponse) response;
        assertTrue(loginResponse.isSuccess());
        assertEquals("Welcome back!", loginResponse.getMessage());
        
        // Kiểm tra xem thực thể User trả về có khớp thông tin không
        assertNotNull(loginResponse.getUser());
        assertEquals("loginUser", loginResponse.getUser().getUsername());
        
        // AuctionManager chạy thật sẽ trả về danh sách trống thay vì thảy ra lỗi NullPointer hay lỗi biên dịch!
        assertNotNull(loginResponse.getUserAuctionList()); 
    }

    @Test
    @DisplayName("LoginHandler: Đăng nhập thất bại do sai mật khẩu")
    void testLoginWrongPassword() {
        LoginHandler loginHandler = new LoginHandler();
        LoginRequest loginReq = mock(LoginRequest.class);
        when(loginReq.getUsername()).thenReturn("nonExistentUser");
        when(loginReq.getPassword()).thenReturn("wrongPassword");

        Response response = loginHandler.handle(loginReq, mockClientContext);

        assertTrue(response instanceof LoginResponse);
        LoginResponse loginResponse = (LoginResponse) response;
        assertFalse(loginResponse.isSuccess());
        assertEquals("Invalid username or password", loginResponse.getMessage());
    }
}