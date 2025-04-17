package org.esprit.utils;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Utility class for time-related operations
 */
public class TimeUtils {
    
    /**
     * Formats the remaining time until a target date as a countdown string
     * @param endTime The target end time
     * @return Formatted string like "2d 5h 30m 15s" or "Expired" if the time has passed
     */
    public static String formatCountdown(LocalDateTime endTime) {
        if (endTime == null) {
            return "N/A";
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        if (endTime.isBefore(now)) {
            return "Expired";
        }
        
        Duration duration = Duration.between(now, endTime);
        
        long days = duration.toDays();
        duration = duration.minusDays(days);
        
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        
        long minutes = duration.toMinutes();
        duration = duration.minusMinutes(minutes);
        
        long seconds = duration.getSeconds();
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        
        sb.append(seconds).append("s");
        
        return sb.toString();
    }
}
