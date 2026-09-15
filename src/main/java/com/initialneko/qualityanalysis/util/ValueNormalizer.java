package com.initialneko.qualityanalysis.util;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.jdbc.LobValue;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Date;

public final class ValueNormalizer {
    private ValueNormalizer() {}

    public static String canonical(Object value, ProfileOptions options) {
        if (value == null) return null;
        if (value instanceof LobValue) return null;
        if (value instanceof BigDecimal) return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        if (value instanceof Number) return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
        if (value instanceof Timestamp) return value.toString();
        if (value instanceof java.sql.Date) return value.toString();
        if (value instanceof Time) return value.toString();
        if (value instanceof Date) return Long.toString(((Date) value).getTime());
        if (value instanceof LocalDate || value instanceof LocalDateTime || value instanceof LocalTime
                || value instanceof OffsetDateTime || value instanceof OffsetTime) return value.toString();
        if (value instanceof byte[]) return "<binary:" + ((byte[]) value).length + ">";
        String s = String.valueOf(value);
        return options.isTrimStrings() ? s.trim() : s;
    }

    public static Object comparable(Object value) {
        if (value == null || value instanceof LobValue) return null;
        if (value instanceof BigDecimal) return value;
        if (value instanceof Number) return new BigDecimal(value.toString());
        if (value instanceof Date) return Long.valueOf(((Date) value).getTime());
        if (value instanceof LocalDate || value instanceof LocalDateTime || value instanceof LocalTime
                || value instanceof OffsetDateTime || value instanceof OffsetTime) return value;
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int compareComparable(Object a, Object b) {
        return ((Comparable) a).compareTo(b);
    }
}
