package com.groupproject.client.utils;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeUtil {
    // Stores the exact difference between the Client's PC and the Server
    private static Duration clockOffset = Duration.ZERO; 

    public static void syncWithServer(LocalDateTime serverTime) {
        if (serverTime != null) {
            // Formula: Server Time - My PC Time = Offset
            clockOffset = Duration.between(LocalDateTime.now(), serverTime);
            ClientLogger.info("Clock synced with server. Offset is " + clockOffset.getSeconds() + " seconds.");
        }
    }

    // ALWAYS use this method instead of LocalDateTime.now() on the client!
    public static LocalDateTime getNow() {
        return LocalDateTime.now().plus(clockOffset);
    }

    public static LocalDateTime getLocalNow(LocalDateTime serverTime) {
        return serverTime.minus(clockOffset);
    }
}
