package com.initialneko.qualityanalysis.persistence;

import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.PatternFrequency;
import com.initialneko.qualityanalysis.model.PresetValidation;
import com.initialneko.qualityanalysis.model.StringShapeStats;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.ValueFrequency;

import java.util.ArrayList;
import java.util.List;

/** Stable persisted representation used by report generation. */
public final class TableProfileRecord {
    public String database;
    public String schema;
    public String table;
    public long scannedAtEpochMillis;
    public long durationMillis;
    public long rowCount;
    public List<ColumnRecord> columns = new ArrayList<ColumnRecord>();

    public static TableProfileRecord from(String database, TableProfile profile,
                                          long scannedAtEpochMillis, long durationMillis) {
        TableProfileRecord record = new TableProfileRecord();
        record.database = database;
        record.schema = profile.getMetadata().getSchema();
        record.table = profile.getMetadata().getTable();
        record.scannedAtEpochMillis = scannedAtEpochMillis;
        record.durationMillis = durationMillis;
        record.rowCount = profile.getRowCount();
        for (ColumnProfile column : profile.getColumns()) {
            record.columns.add(ColumnRecord.from(column));
        }
        return record;
    }

    public static final class ColumnRecord {
        public int ordinal;
        public String name;
        public String label;
        public int jdbcType;
        public String databaseTypeName;
        public int precision;
        public int scale;
        public boolean nullable;
        public boolean declaredPrimaryKey;
        public String family;
        public long rowCount;
        public long nullCount;
        public double nullRate;
        public long blankCount;
        public long semanticNullCount;
        public long nonNullCount;
        public long distinctCount;
        public double uniqueness;
        public String minValue;
        public String maxValue;
        public Long minLength;
        public Long maxLength;
        public Double avgLength;
        public long trimChangedCount;
        public int caseVariantGroupCount;
        public boolean candidatePrimaryKey;
        public boolean constant;
        public boolean quasiConstant;
        public boolean lowCardinality;
        public boolean potentialEnum;
        public boolean lobContentSkipped;
        public List<ValueRecord> values = new ArrayList<ValueRecord>();
        public List<PatternRecord> patterns = new ArrayList<PatternRecord>();
        public ShapeRecord shape;
        public PresetValidationRecord presetValidation;

        static ColumnRecord from(ColumnProfile profile) {
            ColumnMetadata metadata = profile.getMetadata();
            ColumnRecord record = new ColumnRecord();
            record.ordinal = metadata.getOrdinal();
            record.name = metadata.getName();
            record.label = metadata.getLabel();
            record.jdbcType = metadata.getJdbcType();
            record.databaseTypeName = metadata.getDatabaseTypeName();
            record.precision = metadata.getPrecision();
            record.scale = metadata.getScale();
            record.nullable = metadata.isNullable();
            record.declaredPrimaryKey = metadata.isDeclaredPrimaryKey();
            record.family = metadata.getFamily() == null ? null : metadata.getFamily().name();
            record.rowCount = profile.getRowCount();
            record.nullCount = profile.getNullCount();
            record.nullRate = profile.getNullRate();
            record.blankCount = profile.getBlankCount();
            record.semanticNullCount = profile.getSemanticNullCount();
            record.nonNullCount = profile.getNonNullCount();
            record.distinctCount = profile.getDistinctCount();
            record.uniqueness = profile.getUniqueness();
            record.minValue = profile.getMinValue();
            record.maxValue = profile.getMaxValue();
            record.minLength = profile.getMinLength();
            record.maxLength = profile.getMaxLength();
            record.avgLength = profile.getAvgLength();
            record.trimChangedCount = profile.getTrimChangedCount();
            record.caseVariantGroupCount = profile.getCaseVariantGroupCount();
            record.candidatePrimaryKey = profile.isCandidatePrimaryKey();
            record.constant = profile.isConstant();
            record.quasiConstant = profile.isQuasiConstant();
            record.lowCardinality = profile.isLowCardinality();
            record.potentialEnum = profile.isPotentialEnum();
            record.lobContentSkipped = profile.isLobContentSkipped();
            for (ValueFrequency value : profile.getValues()) {
                record.values.add(new ValueRecord(value.getValue(), value.getCount(), value.getRatio()));
            }
            for (PatternFrequency pattern : profile.getTopPatterns()) {
                record.patterns.add(new PatternRecord(pattern.getPattern(), pattern.getCount()));
            }
            if (profile.getStringShapeStats() != null) {
                record.shape = ShapeRecord.from(profile.getStringShapeStats());
            }
            if (profile.getPresetValidation() != null) {
                record.presetValidation = PresetValidationRecord.from(profile.getPresetValidation());
            }
            return record;
        }
    }

    public static final class ValueRecord {
        public String value;
        public long count;
        public double ratio;

        public ValueRecord() { }

        public ValueRecord(String value, long count, double ratio) {
            this.value = value;
            this.count = count;
            this.ratio = ratio;
        }
    }

    public static final class PatternRecord {
        public String pattern;
        public long count;

        public PatternRecord() { }

        public PatternRecord(String pattern, long count) {
            this.pattern = pattern;
            this.count = count;
        }
    }

    public static final class ShapeRecord {
        public long numericOnly;
        public long alphabeticOnly;
        public long alphanumericOnly;
        public long chineseOnly;
        public long containsWhitespace;
        public long containsSpecial;
        public long mixed;

        static ShapeRecord from(StringShapeStats stats) {
            ShapeRecord record = new ShapeRecord();
            record.numericOnly = stats.getNumericOnly();
            record.alphabeticOnly = stats.getAlphabeticOnly();
            record.alphanumericOnly = stats.getAlphanumericOnly();
            record.chineseOnly = stats.getChineseOnly();
            record.containsWhitespace = stats.getContainsWhitespace();
            record.containsSpecial = stats.getContainsSpecial();
            record.mixed = stats.getMixed();
            return record;
        }
    }

    public static final class PresetValidationRecord {
        public String presetTypeName;
        public String presetDescription;
        public long matchedCount;
        public long unmatchedCount;
        public double matchRate;
        public List<String> unmatchedSamples = new ArrayList<String>();

        public PresetValidationRecord() { }

        static PresetValidationRecord from(PresetValidation validation) {
            PresetValidationRecord record = new PresetValidationRecord();
            record.presetTypeName = validation.getPresetTypeName();
            record.presetDescription = validation.getPresetDescription();
            record.matchedCount = validation.getMatchedCount();
            record.unmatchedCount = validation.getUnmatchedCount();
            record.matchRate = validation.getMatchRate();
            if (validation.getUnmatchedSamples() != null) {
                record.unmatchedSamples.addAll(validation.getUnmatchedSamples());
            }
            return record;
        }
    }
}
