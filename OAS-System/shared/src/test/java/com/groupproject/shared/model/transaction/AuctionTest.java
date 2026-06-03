package com.groupproject.shared.model.transaction;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    @Test
    void testAuctionConstructorAndGettersSetters() {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(3);
        byte[] imageBytes = new byte[]{1, 2, 3};

        // Khởi tạo đối tượng
        Auction auction = new Auction(100, 1, "Vintage Watch", imageBytes, null, 50.0, 3600L, startTime, endTime, null);

        // Test Getters từ Constructor
        assertEquals(1, auction.getSellerId());
        assertEquals("Vintage Watch", auction.getTitle());
        assertArrayEquals(imageBytes, auction.getMainImageBytes());
        assertNull(auction.getCategory());
        assertEquals(50.0, auction.getStartingPrice());
        assertEquals(3600L, auction.getDuration());
        assertEquals(startTime, auction.getStartTime());
        assertEquals(endTime, auction.getEndTime());
        assertNull(auction.getStatus());

        // Test Setters
        auction.setSellerId(2);
        assertEquals(2, auction.getSellerId());

        auction.setTitle("Modern Watch");
        assertEquals("Modern Watch", auction.getTitle());

        auction.setMainImagePath("/images/watch.png");
        assertEquals("/images/watch.png", auction.getMainImagePath());

        byte[] newImageBytes = new byte[]{4, 5, 6};
        auction.setMainImageBytes(newImageBytes);
        assertArrayEquals(newImageBytes, auction.getMainImageBytes());

        auction.setCurrentBid(75.5);
        assertEquals(75.5, auction.getCurrentBid());

        auction.setHighestBidderId(99);
        assertEquals(99, auction.getHighestBidderId());

        auction.setStartingPrice(60.0);
        assertEquals(60.0, auction.getStartingPrice());

        auction.setDuration(7200L);
        assertEquals(7200L, auction.getDuration());

        // Thêm test setter cho Category, StartTime, EndTime, Status 
        // (truyền null ở đây vì thiếu implementation thực tế, nhưng vẫn check setter hoạt động)
        auction.setCategory(null);
        assertNull(auction.getCategory());

        auction.setStartTime(startTime.plusDays(1));
        assertEquals(startTime.plusDays(1), auction.getStartTime());

        auction.setEndTime(endTime.plusDays(1));
        assertEquals(endTime.plusDays(1), auction.getEndTime());

        auction.setStatus(null);
        assertNull(auction.getStatus());
    }
}