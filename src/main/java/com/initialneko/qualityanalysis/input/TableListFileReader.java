package com.initialneko.qualityanalysis.input;

import com.initialneko.qualityanalysis.model.TableRef;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Reads an explicit UTF-8 table list without discovering database objects. */
public final class TableListFileReader {
    private TableListFileReader() {}

    public static List<TableRef> read(Path file, String defaultSchema) throws IOException {
        if (file == null) throw new IllegalArgumentException("table list file is null");
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("table list file does not exist: " + file);
        }

        String fallbackSchema = normalizeDefaultSchema(defaultSchema);
        List<TableRef> result = new ArrayList<TableRef>();
        Set<String> seen = new LinkedHashSet<String>();

        BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        try {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.length() > 0 && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }
                String value = line.trim();
                if (value.length() == 0 || value.startsWith("#")) continue;

                TableRef table = parse(value, fallbackSchema, lineNumber);
                String key = table.getSchema().toUpperCase(Locale.ROOT) + "\u0000"
                        + table.getTable().toUpperCase(Locale.ROOT);
                if (seen.add(key)) result.add(table);
            }
        } finally {
            reader.close();
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException("table list file contains no tables: " + file);
        }
        return result;
    }

    private static TableRef parse(String value, String defaultSchema, int lineNumber) {
        int firstDot = value.indexOf('.');
        if (firstDot < 0) {
            if (defaultSchema == null) {
                throw new IllegalArgumentException("line " + lineNumber
                        + " must use SCHEMA.TABLE because no default schema was provided: " + value);
            }
            return new TableRef(defaultSchema, value);
        }
        if (firstDot == 0 || firstDot == value.length() - 1 || value.indexOf('.', firstDot + 1) >= 0) {
            throw new IllegalArgumentException("invalid table entry at line " + lineNumber + ": " + value);
        }
        String schema = value.substring(0, firstDot).trim();
        String table = value.substring(firstDot + 1).trim();
        if (schema.length() == 0 || table.length() == 0) {
            throw new IllegalArgumentException("invalid table entry at line " + lineNumber + ": " + value);
        }
        return new TableRef(schema, table);
    }

    private static String normalizeDefaultSchema(String schema) {
        if (schema == null) return null;
        String value = schema.trim();
        if (value.length() == 0 || "-".equals(value)) return null;
        return value;
    }
}
