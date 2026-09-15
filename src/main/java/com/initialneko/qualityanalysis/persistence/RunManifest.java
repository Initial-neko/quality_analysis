package com.initialneko.qualityanalysis.persistence;

import com.initialneko.qualityanalysis.config.ProfileOptions;

import java.util.ArrayList;
import java.util.List;

/** Durable run metadata. Never stores credentials. */
public final class RunManifest {
    public String runId;
    public String database;
    public long startedAtEpochMillis;
    public long finishedAtEpochMillis;
    public String status;
    public int totalTables;
    public int successTables;
    public int failedTables;
    public OptionsSnapshot options;
    public List<TableRunEntry> tables = new ArrayList<TableRunEntry>();

    public static RunManifest start(String runId, String database, int totalTables, ProfileOptions options) {
        RunManifest manifest = new RunManifest();
        manifest.runId = runId;
        manifest.database = database;
        manifest.startedAtEpochMillis = System.currentTimeMillis();
        manifest.status = "RUNNING";
        manifest.totalTables = totalTables;
        manifest.options = OptionsSnapshot.from(options);
        return manifest;
    }

    public void tableSucceeded(String schema, String table, String recordFile, long rowCount, long durationMillis) {
        tables.add(TableRunEntry.success(schema, table, recordFile, rowCount, durationMillis));
        successTables++;
    }

    public void tableFailed(String schema, String table, String message) {
        tables.add(TableRunEntry.failure(schema, table, message));
        failedTables++;
    }

    public void finish() {
        finishedAtEpochMillis = System.currentTimeMillis();
        status = failedTables == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS";
    }

    public static final class TableRunEntry {
        public String schema;
        public String table;
        public String status;
        public String recordFile;
        public long rowCount;
        public long durationMillis;
        public String error;

        static TableRunEntry success(String schema, String table, String recordFile,
                                     long rowCount, long durationMillis) {
            TableRunEntry entry = new TableRunEntry();
            entry.schema = schema;
            entry.table = table;
            entry.status = "SUCCESS";
            entry.recordFile = recordFile;
            entry.rowCount = rowCount;
            entry.durationMillis = durationMillis;
            return entry;
        }

        static TableRunEntry failure(String schema, String table, String error) {
            TableRunEntry entry = new TableRunEntry();
            entry.schema = schema;
            entry.table = table;
            entry.status = "FAILED";
            entry.error = error;
            return entry;
        }
    }

    public static final class OptionsSnapshot {
        public int fetchSize;
        public int enumPrintThreshold;
        public int lowCardinalityTrackingLimit;
        public int topValueLimit;
        public int clobPreviewChars;
        public boolean trimStrings;
        public boolean distinctEnabled;
        public boolean valueFrequencyEnabled;
        public boolean rangeEnabled;
        public boolean lengthEnabled;
        public boolean patternProfileEnabled;
        public boolean stringShapeEnabled;
        public boolean caseVariantTrackingEnabled;
        public boolean relationshipFingerprintEnabled;

        static OptionsSnapshot from(ProfileOptions options) {
            OptionsSnapshot snapshot = new OptionsSnapshot();
            snapshot.fetchSize = options.getFetchSize();
            snapshot.enumPrintThreshold = options.getEnumPrintThreshold();
            snapshot.lowCardinalityTrackingLimit = options.getLowCardinalityTrackingLimit();
            snapshot.topValueLimit = options.getTopValueLimit();
            snapshot.clobPreviewChars = options.getClobPreviewChars();
            snapshot.trimStrings = options.isTrimStrings();
            snapshot.distinctEnabled = options.isDistinctEnabled();
            snapshot.valueFrequencyEnabled = options.isValueFrequencyEnabled();
            snapshot.rangeEnabled = options.isRangeEnabled();
            snapshot.lengthEnabled = options.isLengthEnabled();
            snapshot.patternProfileEnabled = options.isPatternProfileEnabled();
            snapshot.stringShapeEnabled = options.isStringShapeEnabled();
            snapshot.caseVariantTrackingEnabled = options.isCaseVariantTrackingEnabled();
            snapshot.relationshipFingerprintEnabled = options.isRelationshipFingerprintEnabled();
            return snapshot;
        }
    }
}
