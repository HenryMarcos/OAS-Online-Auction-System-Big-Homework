package com.groupproject.shared.model.transaction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationDTOTest {

    @Test
    void testNotificationDTOConstructorAndGetters() {
        NotificationDTO notification = new NotificationDTO(1, "You won the auction!", false);

        assertEquals(1, notification.getId());
        assertEquals("You won the auction!", notification.getMessage());
        assertFalse(notification.isRead());
    }

    @Test
    void testToStringWhenUnread() {
        NotificationDTO notification = new NotificationDTO(2, "New item listed", false);
        assertEquals("[NEW] New item listed", notification.toString());
    }

    @Test
    void testToStringWhenRead() {
        NotificationDTO notification = new NotificationDTO(3, "Payment received", true);
        assertEquals("[Read] Payment received", notification.toString());
    }
}