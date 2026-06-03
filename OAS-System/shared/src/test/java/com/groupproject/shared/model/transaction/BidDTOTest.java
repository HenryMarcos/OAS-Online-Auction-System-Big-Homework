package com.groupproject.shared.model.transaction;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BidDTOTest {

    @Test
    void testBidDTOConstructorAndGetters() {
        LocalDateTime time = LocalDateTime.of(2023, 10, 1, 14, 30, 45);
        BidDTO bid = new BidDTO("User123", 150.5, time);

        assertEquals("User123", bid.getBidderName());
        assertEquals(150.5, bid.getAmount());
        assertEquals(time, bid.getBidTime());
    }

    @Test
    void testGetTimeStringWithValidTime() {
        LocalDateTime time = LocalDateTime.of(2023, 10, 1, 14, 30, 45);
        BidDTO bid = new BidDTO("User123", 150.5, time);

        assertEquals("14:30:45", bid.getTimeString());
    }

    @Test
    void testGetTimeStringWithNullTime() {
        BidDTO bid = new BidDTO("User123", 150.5, null);

        assertEquals("", bid.getTimeString(), "Should return empty string when bidTime is null");
    }
}