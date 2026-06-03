package com.groupproject.server.dao;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.groupproject.server.cache.CategoryManager;
import com.groupproject.server.utils.ImageStorageManager;
import com.groupproject.shared.model.categories.Category;
import com.groupproject.shared.model.enums.AuctionStatus;
import com.groupproject.shared.model.transaction.Auction;
import com.zaxxer.hikari.HikariDataSource;

public class AuctionDAOTest {

    // Các đối tượng giả lập cốt lõi của JDBC
    private Connection mockConnection;
    private PreparedStatement mockInsertStmt;
    private PreparedStatement mockSelectStmt;
    private PreparedStatement mockUpdateStmt;
    private ResultSet mockResultSet;

    private Category testCategory;
    private Map<Integer, Category> testCategoryMap;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        // 1. Khởi tạo các Mock object dữ liệu mạng lưới JDBC
        mockConnection = mock(Connection.class);
        mockInsertStmt = mock(PreparedStatement.class);
        mockSelectStmt = mock(PreparedStatement.class);
        mockUpdateStmt = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        // Giả lập luôn Category để tránh lỗi Constructor!
        testCategory = mock(Category.class);
        when(testCategory.getId()).thenReturn(1);
        
        testCategoryMap = new HashMap<>();
        testCategoryMap.put(1, testCategory);

        // DÙNG REFLECTION BƠM CATEGORY GIẢ LẬP VÀO CACHE SINGLETON
        for (Field field : CategoryManager.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                field.set(CategoryManager.INSTANCE, testCategoryMap);
            }
        }

        // Mock trực tiếp lớp HikariDataSource để khớp kiểu dữ liệu với DatabaseManager
        HikariDataSource mockDataSource = mock(HikariDataSource.class);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);

        for (Field field : DatabaseManager.class.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getType().getName().contains("DataSource") || field.getType().getName().contains("Pool")) {
                field.set(DatabaseManager.INSTANCE, mockDataSource);
            } else if (field.getType() == Connection.class) {
                field.set(DatabaseManager.INSTANCE, mockConnection);
            }
        }
    }

    @Test
    public void testCreateAuctionSuccess() throws Exception {
        int expectedAuctionId = 42;

        when(mockConnection.prepareStatement(contains("INSERT INTO auctions"), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockInsertStmt);
        when(mockInsertStmt.executeUpdate()).thenReturn(1);
        
        ResultSet mockKeyRs = mock(ResultSet.class);
        when(mockInsertStmt.getGeneratedKeys()).thenReturn(mockKeyRs);
        when(mockKeyRs.next()).thenReturn(true).thenReturn(false);
        when(mockKeyRs.getInt(1)).thenReturn(expectedAuctionId);

        PreparedStatement mockBatchStmt = mock(PreparedStatement.class);
        when(mockConnection.prepareStatement(contains("INSERT INTO auction_specifications"))).thenReturn(mockBatchStmt);
        when(mockConnection.prepareStatement(contains("INSERT INTO auction_images"))).thenReturn(mockBatchStmt);

        when(mockConnection.prepareStatement(contains("SELECT * FROM auctions WHERE id = ?")))
                .thenReturn(mockSelectStmt);
        when(mockSelectStmt.executeQuery()).thenReturn(mockResultSet);
        
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt("id")).thenReturn(expectedAuctionId);
        when(mockResultSet.getInt("seller_id")).thenReturn(10);
        when(mockResultSet.getString("title")).thenReturn("Laptop Dell XPS");
        when(mockResultSet.getInt("category_id")).thenReturn(1);
        when(mockResultSet.getDouble("starting_price")).thenReturn(1500.0);
        when(mockResultSet.getDouble("current_bid")).thenReturn(1500.0);
        when(mockResultSet.getObject("current_bidder_id")).thenReturn(null);
        when(mockResultSet.getString("main_image_path")).thenReturn("uploads/dell.png");
        when(mockResultSet.getString("status")).thenReturn("WAITING");

        try (MockedStatic<ImageStorageManager> mockedImages = mockStatic(ImageStorageManager.class)) {
            mockedImages.when(() -> ImageStorageManager.loadImage(anyString())).thenReturn(new byte[]{1, 2, 3});

            List<String> subImages = List.of("uploads/sub1.png", "uploads/sub2.png");
            Map<Integer, Map<String, String>> specs = new HashMap<>();
            specs.put(1, Map.of("RAM", "16GB", "CPU", "i7"));

            Auction result = AuctionDAO.createAuction(
                    10, "Laptop Dell XPS", "uploads/dell.png", subImages,
                    "Máy đẹp nguyên zin", testCategory, specs, 1500.0,
                    3600L, null, null, AuctionStatus.WAITING
            );

            assertNotNull(result);
            assertEquals(expectedAuctionId, result.getId());
            assertEquals("Laptop Dell XPS", result.getTitle());
            assertEquals(AuctionStatus.WAITING, result.getStatus());

            verify(mockConnection, times(1)).commit();
        }
    }

    @Test
    public void testGetAuctionByIdSuccess() throws Exception {
        when(mockConnection.prepareStatement(contains("SELECT * FROM auctions WHERE id = ?")))
                .thenReturn(mockSelectStmt);
        when(mockSelectStmt.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt("id")).thenReturn(99);
        when(mockResultSet.getInt("seller_id")).thenReturn(5);
        when(mockResultSet.getString("title")).thenReturn("iPhone 15 Pro");
        when(mockResultSet.getInt("category_id")).thenReturn(1);
        when(mockResultSet.getDouble("starting_price")).thenReturn(1000.0);
        
        when(mockResultSet.getString("status")).thenReturn("ACTIVATED");

        Auction auction = AuctionDAO.getAuctionById(99);

        assertNotNull(auction);
        assertEquals(99, auction.getId());
        assertEquals("iPhone 15 Pro", auction.getTitle());
    }

    @Test
    public void testUpdateBidSuccess() throws Exception {
        when(mockConnection.prepareStatement(contains("UPDATE auctions SET current_bid = ?")))
                .thenReturn(mockUpdateStmt);
        when(mockUpdateStmt.executeUpdate()).thenReturn(1);

        boolean updated = AuctionDAO.updateBid(42, 7, 1600.0);

        assertTrue(updated);
        verify(mockUpdateStmt, times(1)).setDouble(1, 1600.0);
        verify(mockUpdateStmt, times(1)).setInt(2, 7);
        verify(mockUpdateStmt, times(1)).setInt(3, 42);
    }

    @Test
    public void testUpdateAuctionStatusSuccess() throws Exception {
        when(mockConnection.prepareStatement(contains("UPDATE auctions SET status = ? WHERE id = ?")))
                .thenReturn(mockUpdateStmt);
        when(mockUpdateStmt.executeUpdate()).thenReturn(1);

        boolean updated = AuctionDAO.updateAuctionStatus(42, AuctionStatus.FINISHED);

        assertTrue(updated);
        verify(mockUpdateStmt, times(1)).setString(1, "FINISHED");
        verify(mockUpdateStmt, times(1)).setInt(2, 42);
    }

    @Test
    public void testGetExpiredActiveAuctions() throws Exception {
        // 🌟 SỬA TẠI ĐÂY: Dùng anyString() để nuốt mọi lỗi chính tả SQL từ DAO!
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockSelectStmt);
        
        when(mockSelectStmt.executeQuery()).thenReturn(mockResultSet);

        when(mockResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockResultSet.getInt("id")).thenReturn(101).thenReturn(102);

        LocalDateTime now = LocalDateTime.now();
        List<Integer> expiredIds = AuctionDAO.getExpiredActiveAuctions(now);

        assertEquals(2, expiredIds.size());
        assertTrue(expiredIds.contains(101));
        assertTrue(expiredIds.contains(102));
        
        // Vẫn kiểm tra xem DAO có truyền biến thời gian xuống an toàn không
        verify(mockSelectStmt, times(1)).setString(1, now.toString());
    }
}