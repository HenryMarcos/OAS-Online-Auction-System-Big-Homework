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

    public static String formatTimeRemaining(LocalDateTime endTime) {
        if (endTime == null) return "No End Time";
        
        LocalDateTime syncedNow = getNow();
        Duration remaining = Duration.between(syncedNow, endTime);
        
        if (remaining.isNegative() || remaining.isZero()) {
            return "ENDED";
        }
        
        long totalSeconds = remaining.getSeconds();
        long days    = totalSeconds / 86400;           
        long hours   = (totalSeconds % 86400) / 3600;  
        long minutes = (totalSeconds % 3600) / 60;     
        long seconds = totalSeconds % 60;              

        if (days > 0) {
            return String.format("Ending in: %dd : %02dh : %02dm : %02ds", days, hours, minutes, seconds);
        } else {
            return String.format("Ending in: %02dh : %02dm : %02ds", hours, minutes, seconds);
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
