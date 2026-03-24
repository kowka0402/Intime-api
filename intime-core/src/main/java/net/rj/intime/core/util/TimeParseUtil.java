package net.rj.intime.core.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParseUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhd]?)", Pattern.CASE_INSENSITIVE);

    private TimeParseUtil() {
    }

    public static long parseToSeconds(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("시간 값이 비어 있습니다.");
        }

        String normalized = input.trim().toLowerCase().replace(" ", "");
        long totalSeconds = 0L;
        int matchedLength = 0;

        Matcher matcher = TIME_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String numberPart = matcher.group(1);
            String unitPart = matcher.group(2);

            long value = Long.parseLong(numberPart);
            matchedLength += matcher.group(0).length();

            totalSeconds += switch (unitPart) {
                case "", "s" -> value;
                case "m" -> value * 60L;
                case "h" -> value * 3600L;
                case "d" -> value * 86400L;
                default -> throw new IllegalArgumentException("지원하지 않는 시간 단위입니다: " + unitPart);
            };
        }

        if (matchedLength != normalized.length()) {
            throw new IllegalArgumentException("잘못된 시간 형식입니다: " + input);
        }

        return totalSeconds;
    }
}