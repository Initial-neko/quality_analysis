package com.initialneko.qualityanalysis.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Persists one table record at a time and keeps run metadata durable. */
public final class ProfileRunStore {
    private static final String MANIFEST = "manifest.json";
    private static final String TABLE_DIR = "tables";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path runDirectory;
    private final Path tableDirectory;

    public ProfileRunStore(Path runDirectory) throws IOException {
        if (runDirectory == null) throw new IllegalArgumentException("runDirectory is null");
        this.runDirectory = runDirectory;
        this.tableDirectory = runDirectory.resolve(TABLE_DIR);
        Files.createDirectories(this.tableDirectory);
    }

    public Path getRunDirectory() { return runDirectory; }
    public Path getTableDirectory() { return tableDirectory; }

    public void writeManifest(RunManifest manifest) throws IOException {
        writeJsonAtomically(runDirectory.resolve(MANIFEST), manifest);
    }

    public RunManifest readManifest() throws IOException {
        return readJson(runDirectory.resolve(MANIFEST), RunManifest.class);
    }

    public String writeTableRecord(TableProfileRecord record) throws IOException {
        String fileName = tableFileName(record.schema, record.table);
        writeJsonAtomically(tableDirectory.resolve(fileName), record);
        return TABLE_DIR + "/" + fileName;
    }

    public TableProfileRecord readTableRecord(String schema, String table) throws IOException {
        return readJson(tableDirectory.resolve(tableFileName(schema, table)), TableProfileRecord.class);
    }

    public List<TableProfileRecord> readAllTableRecords() throws IOException {
        if (!Files.exists(tableDirectory)) return Collections.emptyList();
        List<Path> files = new ArrayList<Path>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(tableDirectory, "*.json");
        try {
            for (Path path : stream) files.add(path);
        } finally {
            stream.close();
        }
        Collections.sort(files, new Comparator<Path>() {
            @Override
            public int compare(Path left, Path right) {
                return left.getFileName().toString().compareToIgnoreCase(right.getFileName().toString());
            }
        });
        List<TableProfileRecord> records = new ArrayList<TableProfileRecord>(files.size());
        for (Path file : files) records.add(readJson(file, TableProfileRecord.class));
        return records;
    }

    private <T> T readJson(Path path, Class<T> type) throws IOException {
        Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        try {
            return gson.fromJson(reader, type);
        } finally {
            reader.close();
        }
    }

    private void writeJsonAtomically(Path target, Object value) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName().toString() + ".tmp");
        Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8);
        try {
            gson.toJson(value, writer);
        } finally {
            writer.close();
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String tableFileName(String schema, String table) {
        String prefix = schema == null || schema.trim().length() == 0 ? "DEFAULT" : schema;
        return safe(prefix) + "." + safe(table) + ".json";
    }

    private static String safe(String value) {
        if (value == null || value.length() == 0) return "UNKNOWN";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.') out.append(c);
            else out.append('_');
        }
        return out.toString();
    }
}
