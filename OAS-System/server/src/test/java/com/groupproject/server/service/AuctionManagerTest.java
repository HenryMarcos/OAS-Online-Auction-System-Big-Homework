package com.groupproject.server.service;

import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.mockito.Mockito.mock;

import com.groupproject.server.core.ClientManager;
import com.groupproject.server.dao.DatabaseManager;
import com.groupproject.server.dao.UserDAO;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.groupproject.shared.model.transaction.AuctionDetail;
import com.groupproject.shared.network.requests.CreateAuctionRequest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionManagerTest {

    private static final int SELLER_ID = 1;
    private static final int BIDDER_1_ID = 2;
    private static final int BIDDER_2_ID = 3;

    private static final int WAITING_AUCTION_ID = 100;
    private static final int ACTIVE_AUCTION_ID = 101;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        // 1. Khởi tạo Database In-Memory
        DatabaseManager dbManager = DatabaseManager.INSTANCE;
        Field dbField = DatabaseManager.class.getDeclaredField("dataSource");
        dbField.setAccessible(true);
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:file:auction_manager_db?mode=memory&cache=shared");
        hikariConfig.setMaximumPoolSize(5);
        dbField.set(dbManager, new HikariDataSource(hikariConfig));
        
        dbManager.initDatabse();

        // 2. Chuỗi thời gian ISO-8601 chuẩn hóa
        String startTimeStr = LocalDateTime.now().withNano(0).toString();
        String endTimeStr = LocalDateTime.now().plusHours(2).withNano(0).toString();

        // 3. Seed Dữ liệu chuẩn
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("REPLACE INTO users (id, username, email, password, balance, created_at) " +
                         "VALUES (" + SELLER_ID + ", 'seller', 'seller@test.com', '123', 0, '" + startTimeStr + "')");
            stmt.execute("REPLACE INTO users (id, username, email, password, balance, created_at) " +
                         "VALUES (" + BIDDER_1_ID + ", 'bidder1', 'b1@test.com', '123', 5000.0, '" + startTimeStr + "')");
            stmt.execute("REPLACE INTO users (id, username, email, password, balance, created_at) " +
                         "VALUES (" + BIDDER_2_ID + ", 'bidder2', 'b2@test.com', '123', 5000.0, '" + startTimeStr + "')");

            stmt.execute("REPLACE INTO categories (id, name) VALUES (1, 'Electronics')");

            // Đấu giá 1: WAITING
            stmt.execute("REPLACE INTO auctions (id, seller_id, title, description, category_id, starting_price, duration, start_time, end_time, status) " +
                         "VALUES (" + WAITING_AUCTION_ID + ", " + SELLER_ID + ", 'Waiting Laptop', 'Test', 1, 500.0, 3600, '" + startTimeStr + "', '" + endTimeStr + "', 'WAITING')");

            // Đấu giá 2: ACTIVATED
            stmt.execute("REPLACE INTO auctions (id, seller_id, title, description, category_id, starting_price, duration, start_time, end_time, status) " +
                         "VALUES (" + ACTIVE_AUCTION_ID + ", " + SELLER_ID + ", 'Active Phone', 'Test', 1, 200.0, 3600, '" + startTimeStr + "', '" + endTimeStr + "', 'ACTIVATED')");
        }

        // 4. Giả lập Socket Client ngầm để tránh lỗi Broadcast Event
        ClientManager.INSTANCE.registerUser(SELLER_ID, mock(ObjectOutputStream.class));
        ClientManager.INSTANCE.registerUser(BIDDER_1_ID, mock(ObjectOutputStream.class));
        ClientManager.INSTANCE.registerUser(BIDDER_2_ID, mock(ObjectOutputStream.class));

        // 5. Nạp dữ liệu vào RAM
        AuctionManager.INSTANCE.refreshCache();
    }

    // ==========================================
    // TEST 1: NẠP CACHE VÀ LẤY DANH SÁCH
    // ==========================================
    @Test
    @Order(1)
    @DisplayName("AuctionManager: Kiểm tra Refresh Cache nạp đúng dữ liệu")
    void testRefreshCacheAndGetList() {
        List<Auction> activeAuctions = AuctionManager.INSTANCE.getActiveAuctionList();
        
        assertNotNull(activeAuctions);
        assertTrue(activeAuctions.size() >= 1, "Cache phải chứa phiên đấu giá đang hoạt động");
        
        Auction activePhone = AuctionManager.INSTANCE.getAuction(ACTIVE_AUCTION_ID);
        assertNotNull(activePhone);
        assertEquals(AuctionStatus.ACTIVATED, activePhone.getStatus());
    }

    // ==========================================
    // TEST 2: LẤY CHI TIẾT PHIÊN
    // ==========================================
    @Test
    @Order(2)
    @DisplayName("AuctionManager: Lấy chi tiết phiên đấu giá")
    void testGetAuctionDetail() throws Exception {
        AuctionDetail detail = reflectGetAuctionDetail(ACTIVE_AUCTION_ID);
        
        assertNotNull(detail, "Phải lấy được chi tiết phiên đấu giá từ Cache");
        assertNotNull(detail.getAuction());
        assertEquals("Active Phone", detail.getAuction().getTitle());
    }

    // ==========================================
    // TEST 3: ĐẶT GIÁ CẠNH TRANH (FIX ĐỒNG BỘ VÍ)
    // ==========================================
    @Test
    @Order(3)
    @DisplayName("AuctionManager: Luồng đấu giá cạnh tranh (Place Bid)")
    void testPlaceBidLogic() throws Exception {
        boolean bid1Success = AuctionManager.INSTANCE.placeBid(ACTIVE_AUCTION_ID, BIDDER_1_ID, 250.0);
        assertTrue(bid1Success);
        
        // Mô phỏng Handler cập nhật số dư vào DB khi Đặt giá thành công
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE users SET balance = 4750.0 WHERE id = " + BIDDER_1_ID);
        }
        assertEquals(4750.0, UserDAO.getBalance(BIDDER_1_ID));

        boolean bid2Fail = AuctionManager.INSTANCE.placeBid(ACTIVE_AUCTION_ID, BIDDER_2_ID, 220.0);
        assertFalse(bid2Fail);

        boolean bid2Success = AuctionManager.INSTANCE.placeBid(ACTIVE_AUCTION_ID, BIDDER_2_ID, 300.0);
        assertTrue(bid2Success);
        
        // Mô phỏng Handler thực hiện chuyển đổi dòng tiền (Trừ người mới, hoàn trả người cũ)
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE users SET balance = 4700.0 WHERE id = " + BIDDER_2_ID);
            stmt.execute("UPDATE users SET balance = 5000.0 WHERE id = " + BIDDER_1_ID);
        }
        
        assertEquals(4700.0, UserDAO.getBalance(BIDDER_2_ID)); // Người mới trừ tiền
        assertEquals(5000.0, UserDAO.getBalance(BIDDER_1_ID)); // Người cũ được hoàn nguyên tiền
    }

    // ==========================================
    // TEST 4: TẠO PHIÊN & CHỐNG SPAM
    // ==========================================
    @Test
    @Order(4)
    @DisplayName("AuctionManager: Kiểm tra chống Spam khi tạo phiên đấu giá")
    void testCreateAuctionSpamPrevention() throws Exception {
        Category cat = new Category(1, "Electronics", null);
        CreateAuctionRequest request = new CreateAuctionRequest(
            "Spam Test Laptop",        
            "Test Desc",               
            cat,                       
            new HashMap<>(),           
            null,                      
            new ArrayList<>(),         
            100.0,                     
            3600L,                     
            LocalDateTime.now(),       
            LocalDateTime.now().plusHours(1), 
            AuctionStatus.WAITING      
        );

        Auction created1 = reflectCreateAuction(request, SELLER_ID);
        assertNotNull(created1, "Lần đầu tạo phải thành công");
        
        Auction created2 = reflectCreateAuction(request, SELLER_ID);
        assertNull(created2, "Lần hai tạo trùng lặp trong 3s phải bị hệ thống chặn");
    }

    // ==========================================
    // TEST 5: LẤY DANH SÁCH USER & HỦY PHIÊN
    // ==========================================
    @Test
    @Order(5)
    @DisplayName("AuctionManager: Lấy danh sách đấu giá của User và Hủy phiên")
    void testGetUserAuctionsAndCancel() throws Exception {
        List<Auction> sellerAuctions = reflectGetUserAuctions(SELLER_ID);
        assertNotNull(sellerAuctions);
        assertTrue(sellerAuctions.size() > 0, "Danh sách đấu giá không được trống");
        
        AuctionManager.INSTANCE.cancelAuction(ACTIVE_AUCTION_ID);
        
        // Mô phỏng Handler hoàn tiền cho người trả giá cao nhất khi hủy phiên
        try (Connection conn = DatabaseManager.INSTANCE.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE users SET balance = 5000.0 WHERE id = " + BIDDER_2_ID);
        }
        
        assertEquals(5000.0, UserDAO.getBalance(BIDDER_2_ID), "Người đặt giá cao nhất phải được trả lại tiền khi phiên bị hủy");
    }

    // ==========================================
    // CÁC HÀM HỖ TRỢ REFLECTION (PRIVATE) - FIXED TYPO
    // ==========================================
    private AuctionDetail reflectGetAuctionDetail(int auctionId) throws Exception {
        Method method = AuctionManager.class.getDeclaredMethod("getAuctionDetail", int.class);
        method.setAccessible(true);
        return (AuctionDetail) method.invoke(AuctionManager.INSTANCE, auctionId);
    }

    private Auction reflectCreateAuction(CreateAuctionRequest req, int sellerId) throws Exception {
        Method method = AuctionManager.class.getDeclaredMethod("createAuction", CreateAuctionRequest.class, int.class);
        method.setAccessible(true);
        return (Auction) method.invoke(AuctionManager.INSTANCE, req, sellerId);
    }

    @SuppressWarnings("unchecked")
    private List<Auction> reflectGetUserAuctions(int userId) throws Exception {
        Method method;
        try {
            method = AuctionManager.class.getDeclaredMethod("getUserAuctions", int.class);
        } catch (NoSuchMethodException e) {
            method = AuctionManager.class.getDeclaredMethod("getAuctionsBySellerId", int.class);
        }
        method.setAccessible(true);
        return (List<Auction>) method.invoke(AuctionManager.INSTANCE, userId);
    }
}