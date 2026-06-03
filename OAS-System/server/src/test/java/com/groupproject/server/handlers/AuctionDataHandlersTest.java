package com.groupproject.server.handlers;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.network.requests.ChangeAuctionStatusRequest;
import com.groupproject.shared.network.requests.CreateAuctionRequest;
import com.groupproject.shared.network.requests.GetAuctionDetailRequest;
import com.groupproject.shared.network.requests.GetAuctionRequest;
import com.groupproject.shared.network.responses.ChangeAuctionStatusResponse;
import com.groupproject.shared.network.responses.CreateAuctionResponse;
import com.groupproject.shared.network.responses.GetAuctionDetailResponse;
import com.groupproject.shared.network.responses.GetAuctionResponse;
import com.groupproject.shared.network.responses.Response;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionDataHandlersTest {

    private ClientHandler mockClientContext;
    
    private static final int SELLER_ID = 1;
    private static final int SEEDED_AUCTION_ID = 200;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        // 1. Cấu hình Database ảo SQLite In-Memory
        DatabaseManager dbManager = DatabaseManager.INSTANCE;
        Field dbField = DatabaseManager.class.getDeclaredField("dataSource");
        dbField.setAccessible(true);
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:file:auction_data_db?mode=memory&cache=shared");
        hikariConfig.setMaximumPoolSize(5);
        dbField.set(dbManager, new HikariDataSource(hikariConfig));
        
        // 2. Tạo bảng cấu trúc hệ thống
        dbManager.initDatabse();

        // 🌟 SỬA LỖI TẠI ĐÂY: Chuẩn hóa chuỗi thời gian theo định dạng ISO-8601 (chứa chữ 'T' ở index 10)
        // Lệnh .withNano(0).toString() sẽ sinh ra chuỗi có dạng: "2026-06-03T21:47:27" đúng ý LocalDateTime.parse()
        String startTimeStr = LocalDateTime.now().withNano(0).toString();
        String endTimeStr = LocalDateTime.now().plusHours(1).withNano(0).toString();

        // 3. Chèn dữ liệu mẫu an toàn vào SQLite ngầm định
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("INSERT OR IGNORE INTO users (id, username, email, password, balance, created_at) " +
                         "VALUES (" + SELLER_ID + ", 'seller1', 'seller1@test.com', '123', 0, '" + startTimeStr + "')");
            
            stmt.execute("INSERT OR IGNORE INTO categories (id, name) VALUES (1, 'Electronics')");

            stmt.execute("INSERT OR IGNORE INTO auctions (id, seller_id, title, description, category_id, starting_price, duration, start_time, end_time, status) " +
                         "VALUES (" + SEEDED_AUCTION_ID + ", " + SELLER_ID + ", 'Seeded Phone', 'Test', 1, 100.0, 3600, '" + startTimeStr + "', '" + endTimeStr + "', 'WAITING')");
        }

        // 4. Đồng bộ dữ liệu mới nạp vào Cache của AuctionManager
        AuctionManager.INSTANCE.refreshCache();
    }

    @BeforeEach
    void setUp() {
        mockClientContext = mock(ClientHandler.class);
        // Cung cấp Seller ID tương thích khi Handler yêu cầu context người dùng
        when(mockClientContext.getAuthenticatedUserId()).thenReturn(SELLER_ID);
    }

    // ==========================================
    // 1. TEST TẠO PHIÊN ĐẤU GIÁ
    // ==========================================
    @Test
    @Order(1)
    @DisplayName("CreateAuctionHandler: Tạo phiên đấu giá thành công")
    void testCreateAuctionSuccess() {
        CreateAuctionHandler handler = new CreateAuctionHandler();
        CreateAuctionRequest request = mock(CreateAuctionRequest.class);
        Category mockCategory = new Category(1, "Electronics", null);
        
        when(request.getTitle()).thenReturn("New Laptop M3");
        when(request.getDescription()).thenReturn("Brand new laptop");
        when(request.getCategory()).thenReturn(mockCategory);
        when(request.getCategoryGroupedSpecs()).thenReturn(new HashMap<>());
        when(request.getMainImageBytes()).thenReturn(null);
        when(request.getSubImagesBytes()).thenReturn(new ArrayList<>());
        when(request.getStartingPrice()).thenReturn(1000.0);
        when(request.getDuration()).thenReturn(3600L);
        when(request.getStartTime()).thenReturn(LocalDateTime.now());
        when(request.getEndTime()).thenReturn(LocalDateTime.now().plusHours(1));
        when(request.getStatus()).thenReturn(AuctionStatus.WAITING);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof CreateAuctionResponse);
        CreateAuctionResponse createRes = (CreateAuctionResponse) response;
        assertTrue(createRes.isSuccess(), "Phải tạo phiên đấu giá thành công");
        assertNotNull(createRes.getAuction());
        assertEquals("New Laptop M3", createRes.getAuction().getTitle());
    }

    // ==========================================
    // 2. TEST LẤY DANH SÁCH PHIÊN ĐẤU GIÁ
    // ==========================================
    @Test
    @Order(2)
    @DisplayName("GetAuctionHandler: Lấy danh sách theo Seller ID")
    void testGetAuctionsBySeller() {
        GetAuctionHandler handler = new GetAuctionHandler();
        GetAuctionRequest request = mock(GetAuctionRequest.class);
        
        when(request.getSellerId()).thenReturn(SELLER_ID);
        when(request.getCategoryId()).thenReturn(null); 
        when(request.getStatus()).thenReturn(null);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof GetAuctionResponse);
        GetAuctionResponse getRes = (GetAuctionResponse) response;
        assertTrue(getRes.isSuccess());
        assertNotNull(getRes.getAuctions());
        assertTrue(getRes.getAuctions().size() >= 1); 
    }

    // ==========================================
    // 3. TEST LẤY CHI TIẾT PHIÊN ĐẤU GIÁ
    // ==========================================
    @Test
    @Order(3)
    @DisplayName("GetAuctionDetailHandler: Lấy chi tiết phiên đấu giá thành công")
    void testGetAuctionDetailSuccess() {
        GetAuctionDetailHandler handler = new GetAuctionDetailHandler();
        GetAuctionDetailRequest request = mock(GetAuctionDetailRequest.class);
        
        when(request.getAuctionId()).thenReturn(SEEDED_AUCTION_ID);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof GetAuctionDetailResponse);
        GetAuctionDetailResponse detailRes = (GetAuctionDetailResponse) response;
        assertTrue(detailRes.isSuccess());
        assertNotNull(detailRes.getAuctionDetail());
        assertEquals("Seeded Phone", detailRes.getAuctionDetail().getAuction().getTitle());
    }

    // ==========================================
    // 4. TEST ĐỔI TRẠNG THÁI SANG ACTIVATED
    // ==========================================
    @Test
    @Order(4)
    @DisplayName("ChangeAuctionStatusHandler: Chuyển trạng thái WAITING sang ACTIVATED")
    void testChangeStatusToActivated() {
        ChangeAuctionStatusHandler handler = new ChangeAuctionStatusHandler();
        ChangeAuctionStatusRequest request = mock(ChangeAuctionStatusRequest.class);
        
        when(request.getAuctionId()).thenReturn(SEEDED_AUCTION_ID);
        when(request.getNewStatus()).thenReturn(AuctionStatus.ACTIVATED);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof ChangeAuctionStatusResponse);
        ChangeAuctionStatusResponse statusRes = (ChangeAuctionStatusResponse) response;
        assertTrue(statusRes.isSuccess());
        
        Auction activeAuction = AuctionManager.INSTANCE.getAuction(SEEDED_AUCTION_ID);
        assertNotNull(activeAuction);
        assertEquals(AuctionStatus.ACTIVATED, activeAuction.getStatus());
    }

    // ==========================================
    // 5. TEST HỦY PHIÊN ĐẤU GIÁ
    // ==========================================
    @Test
    @Order(5)
    @DisplayName("ChangeAuctionStatusHandler: Chuyển trạng thái thành CANCELLED")
    void testChangeStatusToCancelled() {
        ChangeAuctionStatusHandler handler = new ChangeAuctionStatusHandler();
        ChangeAuctionStatusRequest request = mock(ChangeAuctionStatusRequest.class);
        
        when(request.getAuctionId()).thenReturn(SEEDED_AUCTION_ID);
        when(request.getNewStatus()).thenReturn(AuctionStatus.CANCELLED);

        Response response = handler.handle(request, mockClientContext);

        assertTrue(response instanceof ChangeAuctionStatusResponse);
        ChangeAuctionStatusResponse statusRes = (ChangeAuctionStatusResponse) response;
        assertTrue(statusRes.isSuccess());
    }
}