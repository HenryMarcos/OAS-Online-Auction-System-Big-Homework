package com.groupproject.server.dao;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.groupproject.shared.model.transaction.BidDTO;
import com.zaxxer.hikari.HikariDataSource;

public class BidDAOTest {

    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    public void setUp() throws Exception {
        // 1. Khởi tạo các Mock object
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        // 2. Cấu hình mặc định: Nuốt mọi câu lệnh SQL (anyString) để chống lỗi chính tả
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // Mặc định Update/Insert luôn thành công 1 dòng

        // 3. Ghi đè DatabaseManager giống y hệt như đã làm với AuctionDAOTest
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
    public void testGetUniqueBidders() throws Exception {
        // Giả lập Database trả về 2 user ID: 15 và 25
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt("bidder_id")).thenReturn(15, 25);

        List<Integer> bidders = BidDAO.getUniqueBidders(1001);

        // Kiểm tra kết quả
        assertEquals(2, bidders.size());
        assertTrue(bidders.contains(15));
        assertTrue(bidders.contains(25));
    }

    @Test
    public void testGetBidsForAuction() throws Exception {
        // Giả lập Database trả về 1 dòng lịch sử đặt giá
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getInt("bidder_id")).thenReturn(99);
        when(mockResultSet.getDouble("amount")).thenReturn(500.5);
        when(mockResultSet.getString("bid_time")).thenReturn("2026-06-03T12:00:00");

        List<BidDTO> bids = BidDAO.getBidsForAuction(1001);

        // Kiểm tra DTO được tạo đúng không
        assertEquals(1, bids.size());
        assertEquals("User 99", bids.get(0).getBidderName());
        assertEquals(500.5, bids.get(0).getAmount());
    }

    @Test
    public void testExecuteDirectTransferSuccess() throws Exception {
        // Chạy hàm chuyển tiền từ winner sang seller
        boolean result = BidDAO.executeDirectTransfer(10, 20, 150.0);

        assertTrue(result);
        
        // Xác nhận Database đã commit thành công
        verify(mockConnection, times(1)).commit();
        // Sẽ có 2 câu lệnh update (1 trừ tiền mua, 1 cộng tiền bán) được thực thi
        verify(mockPreparedStatement, times(2)).executeUpdate();
    }

    @Test
    public void testInsertBidSimpleSuccess() throws Exception {
        // Chạy hàm insertBid cơ bản
        boolean result = BidDAO.insertBid(1001, 5, 200.0);

        assertTrue(result);
        
        // Sẽ có 2 câu lệnh được thực thi: 1 INSERT vào bảng bids, 1 UPDATE giá vào bảng auctions
        verify(mockPreparedStatement, times(2)).executeUpdate();
    }
}