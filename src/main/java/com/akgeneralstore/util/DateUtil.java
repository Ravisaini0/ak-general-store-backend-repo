package com.akgeneralstore.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private DateUtil() {
    }

    public static String format(LocalDateTime value) {
        return value == null ? "" : value.format(FORMATTER);
    }
}
