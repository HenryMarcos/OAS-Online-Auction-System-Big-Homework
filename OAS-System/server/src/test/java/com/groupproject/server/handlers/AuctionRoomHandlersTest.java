package com.groupproject.server.handlers;

import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.groupproject.server.dao.DatabaseManager;
import com.groupproject.server.service.AuctionManager;
import com.groupproject.shared.network.requests.JoinAuctionRequest;
import com.groupproject.shared.network.requests.LeaveAuctionRoomRequest;
import com.groupproject.shared.network.requests.Request;
import com.groupproject.shared.network.responses.JoinAuctionResponse;
import com.groupproject.shared.network.responses.LeaveAuctionRoomResponse;
import com.groupproject.shared.network.responses.Response;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionRoomHandlersTest {

    private ClientHandler mockClientContext;
    private ObjectOutputStream mockOut;
    
    private static final int SEEDED_AUCTION_ID = 300;
    private static final int NON_EXISTENT_AUCTION_ID = 999;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        // 1. Cấu hình Database ảo SQLite In-Memory Shared Cache để nuôi dữ liệu cho AuctionManager
        DatabaseManager dbManager = DatabaseManager.INSTANCE;
        Field dbField = DatabaseManager.class.getDeclaredField("dataSource");
        dbField.setAccessible(true);
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:file:auction_room_db?mode=memory&cache=shared");
        hikariConfig.setMaximumPoolSize(5);
        dbField.set(dbManager, new HikariDataSource(hikariConfig));
        
        dbManager.initDatabse();

        // 🌟 SỬA TẠI ĐÂY: Chuẩn hóa chuỗi thời gian định dạng ISO-8601 (chứa chữ 'T' ở index 10)
        String startTimeStr = LocalDateTime.now().withNano(0).toString();
        String endTimeStr = LocalDateTime.now().plusHours(1).withNano(0).toString();

        // 2. Chèn dữ liệu mẫu: Cần 1 User và 1 Phiên đấu giá đang hoạt động (ACTIVATED)
        try (Connection conn = dbManager.getConnection();
            Statement stmt = conn.createStatement()) {
            
            stmt.execute("INSERT OR IGNORE INTO users (id, username, email, password, balance, created_at) " +
                        "VALUES (1, 'userTest', 'test@test.com', '123', 0, '" + startTimeStr + "')");
            
            stmt.execute("INSERT OR IGNORE INTO categories (id, name) VALUES (1, 'Electronics')");

            // 🌟 SỬA TẠI ĐÂY: Thêm đầy đủ cột start_time và truyền chuỗi thời gian chuẩn ISO-8601 vào câu lệnh SQL
            stmt.execute("INSERT OR IGNORE INTO auctions (id, seller_id, title, description, category_id, starting_price, duration, start_time, end_time, status) " +
                        "VALUES (" + SEEDED_AUCTION_ID + ", 1, 'Room Test Phone', 'Test Room', 1, 100.0, 3600, '" + startTimeStr + "', '" + endTimeStr + "', 'ACTIVATED')");
        }

        // Đồng bộ nạp dữ liệu từ DB RAM vào Cache của AuctionManager công khai
        AuctionManager.INSTANCE.refreshCache();
    }

    @BeforeEach
    void setUp() {
        mockClientContext = mock(ClientHandler.class);
        mockOut = mock(ObjectOutputStream.class);
        
        // Cấu hình context mặc định cho client
        when(mockClientContext.getAuthenticatedUserId()).thenReturn(1);
        when(mockClientContext.getOut()).thenReturn(mockOut);
    }

    // ==========================================
    // 1. TEST JOIN AUCTION HANDLER
    // ==========================================

    @Test
    @Order(1)
    @DisplayName("JoinAuctionHandler: Thất bại khi truyền sai loại Request")
    void testJoinAuctionInvalidRequest() {
        JoinAuctionHandler handler = new JoinAuctionHandler();
        Request genericRequest = mock(Request.class); // Request chung chứ không phải JoinAuctionRequest

        Response response = handler.handle(genericRequest, mockClientContext);

        assertTrue(response instanceof JoinAuctionResponse);
        JoinAuctionResponse joinRes = (JoinAuctionResponse) response;
        assertFalse(joinRes.isSuccess());
        assertEquals("Invalid Request", joinRes.getMessage());
    }

    @Test
    @Order(2)
    @DisplayName("JoinAuctionHandler: Vào phòng thành công khi đấu giá tồn tại")
    void testJoinAuctionSuccess() {
        JoinAuctionHandler handler = new JoinAuctionHandler();
        JoinAuctionRequest request = mock(JoinAuctionRequest.class);
        when(request.getAuctionId()).thenReturn(SEEDED_AUCTION_ID);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof JoinAuctionResponse);
        JoinAuctionResponse joinRes = (JoinAuctionResponse) response;
        
        assertTrue(joinRes.isSuccess());
        assertNotNull(joinRes.getAuctionDetail());
        assertEquals("Successfully joined auction", joinRes.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("JoinAuctionHandler: Thất bại khi phòng đấu giá không tồn tại")
    void testJoinAuctionNotFound() {
        JoinAuctionHandler handler = new JoinAuctionHandler();
        JoinAuctionRequest request = mock(JoinAuctionRequest.class);
        when(request.getAuctionId()).thenReturn(NON_EXISTENT_AUCTION_ID);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof JoinAuctionResponse);
        JoinAuctionResponse joinRes = (JoinAuctionResponse) response;
        assertFalse(joinRes.isSuccess());
        assertEquals("Auction not found or no longer active.", joinRes.getMessage());
    }

    // ==========================================
    // 2. TEST LEAVE AUCTION ROOM HANDLER
    // ==========================================

    @Test
    @Order(4)
    @DisplayName("LeaveAuctionRoomHandler: Thất bại khi truyền sai loại Request")
    void testLeaveRoomInvalidRequest() {
        LeaveAuctionRoomHandler handler = new LeaveAuctionRoomHandler();
        Request genericRequest = mock(Request.class);

        Response response = handler.handle(genericRequest, mockClientContext);

        assertTrue(response instanceof LeaveAuctionRoomResponse);
        LeaveAuctionRoomResponse leaveRes = (LeaveAuctionRoomResponse) response;
        assertFalse(leaveRes.isSuccess());
        assertEquals("Invalid request format", leaveRes.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("LeaveAuctionRoomHandler: Rời phòng thành công khi Stream đầu ra hợp lệ")
    void testLeaveRoomSuccess() {
        LeaveAuctionRoomHandler handler = new LeaveAuctionRoomHandler();
        LeaveAuctionRoomRequest request = mock(LeaveAuctionRoomRequest.class);
        when(request.getAuctionId()).thenReturn(SEEDED_AUCTION_ID);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof LeaveAuctionRoomResponse);
        LeaveAuctionRoomResponse leaveRes = (LeaveAuctionRoomResponse) response;
        assertTrue(leaveRes.isSuccess());
        assertEquals("Left room " + SEEDED_AUCTION_ID + " successfully.", leaveRes.getMessage());
    }

    @Test
    @Order(6)
    @DisplayName("LeaveAuctionRoomHandler: Thất bại khi Stream đầu ra (Out) bị null")
    void testLeaveRoomNullOut() {
        LeaveAuctionRoomHandler handler = new LeaveAuctionRoomHandler();
        LeaveAuctionRoomRequest request = mock(LeaveAuctionRoomRequest.class);
        when(request.getAuctionId()).thenReturn(SEEDED_AUCTION_ID);
        
        // Ép tình huống dòng truyền dữ liệu mạng của Client bị ngắt (null)
        when(mockClientContext.getOut()).thenReturn(null);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof LeaveAuctionRoomResponse);
        LeaveAuctionRoomResponse leaveRes = (LeaveAuctionRoomResponse) response;
        assertFalse(leaveRes.isSuccess());
        assertEquals("Internal Server Error: No Output Stream found.", leaveRes.getMessage());
    }
}