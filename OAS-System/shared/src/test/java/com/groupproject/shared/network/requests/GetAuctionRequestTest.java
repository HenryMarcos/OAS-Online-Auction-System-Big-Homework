package com.groupproject.shared.network.requests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// 🌟 THÊM PUBLIC VÀO ĐÂY
public class GetAuctionRequestTest {

    @Test
    public void testEmptyConstructor() { // 🌟 THÊM PUBLIC
        // Test constructor rỗng phục vụ Serialization
        GetAuctionRequest request = new GetAuctionRequest();
        assertNull(request.getSellerId());
        assertNull(request.getStatus());
        assertNull(request.getCategoryId());
    }

    @Test
    public void testMainConstructorAndGetters() { // 🌟 THÊM PUBLIC
        // Test constructor chính
        GetAuctionRequest request = new GetAuctionRequest(123, null, 5);
        assertEquals(123, request.getSellerId());
        assertNull(request.getStatus());
        assertEquals(5, request.getCategoryId());
    }

    @Test
    public void testGetByStatus() { // 🌟 THÊM PUBLIC
        // Test hàm static factory tạo nhanh theo trạng thái
        GetAuctionRequest request = GetAuctionRequest.getByStatus(null, 10);
        assertNull(request.getSellerId());
        assertNull(request.getStatus());
        assertEquals(10, request.getCategoryId());
    }

    @Test
    public void testGetBySeller() { // 🌟 THÊM PUBLIC
        // Test hàm static factory tạo nhanh theo người bán
        GetAuctionRequest request = GetAuctionRequest.getBySeller(99, 20);
        assertEquals(99, request.getSellerId());
        assertNull(request.getStatus());
        assertEquals(20, request.getCategoryId());
    }

    @Test
    public void testGetAll() { // 🌟 THÊM PUBLIC
        // Test hàm static factory lấy tất cả
        GetAuctionRequest request = GetAuctionRequest.getAll();
        assertNull(request.getSellerId());
        assertNull(request.getStatus());
        assertNull(request.getCategoryId());
    }
}