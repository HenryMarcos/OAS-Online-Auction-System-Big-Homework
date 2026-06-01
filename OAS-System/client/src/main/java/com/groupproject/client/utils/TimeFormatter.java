package com.groupproject.client.utils;

public class TimeFormatter {
    public static String formatTimeLeft(long totalSeconds) {
        long days    = totalSeconds / 86400;
        long hours   = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        return String.format("Ending in: %dd : %02dh : %02dm : %02ds", days, hours, minutes, seconds);
    }
}
