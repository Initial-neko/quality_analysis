package com.initialneko.qualityanalysis.jdbc;

import java.util.regex.Pattern;

public final class SqlTableName {
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9_$#]+", Pattern.UNICODE_CASE);

    private SqlTableName() {}

    public static String qualified(String schema, String table) {
        requireSafe(table, "table");
        if (schema == null || schema.trim().length() == 0) return table;
        requireSafe(schema, "schema");
        return schema + "." + table;
    }

    private static void requireSafe(String value, String label) {
        if (value == null || !SAFE.matcher(value).matches()) throw new IllegalArgumentException(label + " contains unsupported characters: " + value);
    }
}
