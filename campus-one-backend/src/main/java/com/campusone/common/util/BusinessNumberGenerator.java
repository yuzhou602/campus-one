package com.campusone.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Generates readable, collision-resistant identifiers for externally visible business records. */
public final class BusinessNumberGenerator {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private BusinessNumberGenerator() {
    }

    public static String generate(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("业务编号前缀不能为空");
        }
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 6).toUpperCase();
        return prefix + LocalDateTime.now().format(TIMESTAMP) + suffix;
    }
}
