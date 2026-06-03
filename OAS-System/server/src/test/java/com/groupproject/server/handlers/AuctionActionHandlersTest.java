package com.groupproject.server.handlers;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.groupproject.server.core.ClientHandler;
import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.DatabaseManager;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.shared.network.requests.PlaceBidRequest;
import com.groupproject.shared.network.requests.TopUpRequest;
import com.groupproject.shared.network.requests.UnwatchAuctionRequest;
import com.groupproject.shared.network.requests.WatchAuctionRequest;
import com.groupproject.shared.network.responses.PlaceBidResponse;
import com.groupproject.shared.network.responses.Response;
import com.groupproject.shared.network.responses.TopUpResponse;
import com.groupproject.shared.network.responses.UnwatchAuctionResponse;
import com.groupproject.shared.network.responses.WatchAuctionResponse;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionActionHandlersTest {

    private ClientHandler mockClientContext;
    private ObjectOutputStream mockOutStream;

    // ID cố định dùng cho việc test
    private static final int SELLER_ID = 1;
    private static final int BIDDER_ID = 2;
    private static final int TEST_AUCTION_ID = 100;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        // 1. Cấu hình Database ảo trên RAM
        DatabaseManager dbManager = DatabaseManager.INSTANCE;
        Field dbField = DatabaseManager.class.getDeclaredField("dataSource");
        dbField.setAccessible(true);
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:file:action_handlers_db?mode=memory&cache=shared");
        hikariConfig.setMaximumPoolSize(5);
        dbField.set(dbManager, new HikariDataSource(hikariConfig));
        
        // 2. Tạo bảng
        dbManager.initDatabse();

        // 3. Xử lý thời gian kết thúc chuẩn hóa theo múi giờ Java
        LocalDateTime endTime = LocalDateTime.now().plusHours(2);
        String endTimeStr = endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 4. Seed dữ liệu mẫu (Sử dụng REPLACE INTO để an toàn tuyệt đối)
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Tạo Seller
            stmt.execute("REPLACE INTO users (id, username, email, password, balance, created_at) " +
                         "VALUES (" + SELLER_ID + ", 'seller', 'seller@test.com', '123', 0, CURRENT_TIMESTAMP)");
            
            // Tạo Bidder (Người mua)
            stmt.execute("REPLACE INTO users (id, username, email, password, balance, created_at) " +
                         "VALUES (" + BIDDER_ID + ", 'bidder', 'bidder@test.com', '123', 10000, CURRENT_TIMESTAMP)");
            
            // Tạo Danh mục
            stmt.execute("REPLACE INTO categories (id, name) VALUES (1, 'Test Category')");

            // ĐÃ SỬA: Bỏ cột start_time không tồn tại, chỉ giữ lại end_time
            stmt.execute("REPLACE INTO auctions (id, seller_id, title, description, category_id, starting_price, duration, end_time, status) " +
                         "VALUES (" + TEST_AUCTION_ID + ", " + SELLER_ID + ", 'Laptop Gaming', 'New', 1, 500.0, 3600, '" + endTimeStr + "', 'ACTIVATED')");
        }

        // 5. Nạp dữ liệu vào Cache
        AuctionManager.INSTANCE.refreshCache();
    }

    @BeforeEach
    void setUp() throws Exception {
        mockClientContext = mock(ClientHandler.class);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mockOutStream = new ObjectOutputStream(baos);
        
        when(mockClientContext.getOut()).thenReturn(mockOutStream);
        when(mockClientContext.getAuthenticatedUserId()).thenReturn(BIDDER_ID);

        ClientManager.INSTANCE.registerUser(BIDDER_ID, mockOutStream);
    }

    // ==========================================
    // 1. TEST CHO TOPUP HANDLER
    // ==========================================
    
    @Test
    @Order(1)
    @DisplayName("TopUpHandler: Nạp tiền thành công")
    void testTopUpSuccess() {
        TopUpHandler handler = new TopUpHandler();
        TopUpRequest request = mock(TopUpRequest.class);
        when(request.getAmount()).thenReturn(500.0);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof TopUpResponse);
        TopUpResponse topUpRes = (TopUpResponse) response;
        assertTrue(topUpRes.isSuccess());
        assertEquals(10500.0, topUpRes.getNewBalance()); 
    }

    @Test
    @Order(2)
    @DisplayName("TopUpHandler: Thất bại do nạp số tiền âm")
    void testTopUpInvalidAmount() {
        TopUpHandler handler = new TopUpHandler();
        TopUpRequest request = mock(TopUpRequest.class);
        when(request.getAmount()).thenReturn(-50.0);

        Response response = handler.handle(request, mockClientContext);

        TopUpResponse topUpRes = (TopUpResponse) response;
        assertFalse(topUpRes.isSuccess());
        assertEquals("Số tiền nạp phải lớn hơn 0.", topUpRes.getMessage());
    }

    // ==========================================
    // 2. TEST CHO WATCH & UNWATCH HANDLER
    // ==========================================

    @Test
    @Order(3)
    @DisplayName("WatchAuctionHandler: Thất bại do sai ID")
    void testWatchAuctionNotFound() {
        WatchAuctionHandler handler = new WatchAuctionHandler();
        WatchAuctionRequest request = mock(WatchAuctionRequest.class);
        when(request.getAuctionId()).thenReturn(9999); 

        Response response = handler.handle(request, mockClientContext);

        WatchAuctionResponse watchRes = (WatchAuctionResponse) response;
        assertFalse(watchRes.isSuccess());
    }

    @Test
    @Order(4)
    @DisplayName("UnwatchAuctionHandler: Hủy theo dõi thành công")
    void testUnwatchAuctionSuccess() {
        UnwatchAuctionHandler handler = new UnwatchAuctionHandler();
        UnwatchAuctionRequest request = mock(UnwatchAuctionRequest.class);
        when(request.getAuctionId()).thenReturn(TEST_AUCTION_ID);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof UnwatchAuctionResponse);
        UnwatchAuctionResponse unwatchRes = (UnwatchAuctionResponse) response;
        assertTrue(unwatchRes.isSuccess());
        assertEquals("Hủy đăng ký theo dõi thành công.", unwatchRes.getMessage());
    }

    // ==========================================
    // 3. TEST CHO PLACE BID HANDLER
    // ==========================================

    @Test
    @Order(5)
    @DisplayName("PlaceBidHandler: Thất bại do đặt giá thấp hơn giá hiện tại")
    void testPlaceBidTooLow() {
        PlaceBidHandler handler = new PlaceBidHandler();
        PlaceBidRequest request = mock(PlaceBidRequest.class);
        
        when(request.getAuctionId()).thenReturn(TEST_AUCTION_ID);
        when(request.getBidAmount()).thenReturn(550.0); 

        Response response = handler.handle(request, mockClientContext);

        PlaceBidResponse bidRes = (PlaceBidResponse) response;
        assertFalse(bidRes.isSuccess(), "Phải thất bại vì giá mới thấp hơn giá cao nhất hiện tại");
    }
}