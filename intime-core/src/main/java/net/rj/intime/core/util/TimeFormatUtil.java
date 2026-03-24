package net.rj.intime.core.util;

public final class TimeFormatUtil {

    private TimeFormatUtil() {
    }

    public static String formatFixed(long totalSeconds) {
        long safe = Math.max(0L, totalSeconds);

        long days = safe / 86400L;
        long hours = (safe % 86400L) / 3600L;
        long minutes = (safe % 3600L) / 60L;
        long seconds = safe % 60L;

        return String.format("%02dd%02dh%02dm%02ds", days, hours, minutes, seconds);
    }

    public static String formatSmart(long totalSeconds) {
        long safe = Math.max(0L, totalSeconds);

        long days = safe / 86400L;
        long hours = (safe % 86400L) / 3600L;
        long minutes = (safe % 3600L) / 60L;
        long seconds = safe % 60L;

        if (days > 0L) {
            return String.format("%02dd%02dh%02dm%02ds", days, hours, minutes, seconds);
        }
        if (hours > 0L) {
            return String.format("%02dh%02dm%02ds", hours, minutes, seconds);
        }
        if (minutes > 0L) {
            return String.format("%02dm%02ds", minutes, seconds);
        }
        return String.format("%02ds", seconds);
    }
}
