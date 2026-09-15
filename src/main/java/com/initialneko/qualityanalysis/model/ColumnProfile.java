package com.initialneko.qualityanalysis.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ColumnProfile {
    private final ColumnMetadata metadata;
    private final long rowCount;
    private final long nullCount;
    private final long blankCount;
    private final long semanticNullCount;
    private final long nonNullCount;
    private final long distinctCount;
    private final double uniqueness;
    private final String minValue;
    private final String maxValue;
    private final Long minLength;
    private final Long maxLength;
    private final Double avgLength;
    private final long trimChangedCount;
    private final int caseVariantGroupCount;
    private final boolean candidatePrimaryKey;
    private final boolean constant;
    private final boolean quasiConstant;
    private final boolean lowCardinality;
    private final boolean lobContentSkipped;
    private final List<ValueFrequency> values;
    private final List<PatternFrequency> topPatterns;
    private final StringShapeStats stringShapeStats;
    private final SetFingerprint setFingerprint;

    public ColumnProfile(ColumnMetadata metadata, long rowCount, long nullCount, long blankCount,
                         long semanticNullCount, long nonNullCount, long distinctCount, double uniqueness,
                         String minValue, String maxValue, Long minLength, Long maxLength, Double avgLength,
                         long trimChangedCount, int caseVariantGroupCount, boolean candidatePrimaryKey,
                         boolean constant, boolean quasiConstant, boolean lowCardinality,
                         boolean lobContentSkipped, List<ValueFrequency> values,
                         List<PatternFrequency> topPatterns, StringShapeStats stringShapeStats,
                         SetFingerprint setFingerprint) {
        this.metadata = metadata;
        this.rowCount = rowCount;
        this.nullCount = nullCount;
        this.blankCount = blankCount;
        this.semanticNullCount = semanticNullCount;
        this.nonNullCount = nonNullCount;
        this.distinctCount = distinctCount;
        this.uniqueness = uniqueness;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.avgLength = avgLength;
        this.trimChangedCount = trimChangedCount;
        this.caseVariantGroupCount = caseVariantGroupCount;
        this.candidatePrimaryKey = candidatePrimaryKey;
        this.constant = constant;
        this.quasiConstant = quasiConstant;
        this.lowCardinality = lowCardinality;
        this.lobContentSkipped = lobContentSkipped;
        this.values = Collections.unmodifiableList(new ArrayList<ValueFrequency>(values));
        this.topPatterns = Collections.unmodifiableList(new ArrayList<PatternFrequency>(topPatterns));
        this.stringShapeStats = stringShapeStats;
        this.setFingerprint = setFingerprint;
    }

    public ColumnMetadata getMetadata() { return metadata; }
    public long getRowCount() { return rowCount; }
    public long getNullCount() { return nullCount; }
    public long getBlankCount() { return blankCount; }
    public long getSemanticNullCount() { return semanticNullCount; }
    public long getNonNullCount() { return nonNullCount; }
    public long getDistinctCount() { return distinctCount; }
    public double getUniqueness() { return uniqueness; }
    public double getNullRate() { return rowCount == 0 ? 0.0d : (double) nullCount / (double) rowCount; }
    public String getMinValue() { return minValue; }
    public String getMaxValue() { return maxValue; }
    public Long getMinLength() { return minLength; }
    public Long getMaxLength() { return maxLength; }
    public Double getAvgLength() { return avgLength; }
    public long getTrimChangedCount() { return trimChangedCount; }
    public int getCaseVariantGroupCount() { return caseVariantGroupCount; }
    public boolean isCandidatePrimaryKey() { return candidatePrimaryKey; }
    public boolean isConstant() { return constant; }
    public boolean isQuasiConstant() { return quasiConstant; }
    public boolean isLowCardinality() { return lowCardinality; }
    public boolean isPotentialEnum() { return !lobContentSkipped && distinctCount > 0 && distinctCount <= 20; }
    public boolean isLobContentSkipped() { return lobContentSkipped; }
    public List<ValueFrequency> getValues() { return values; }
    public List<PatternFrequency> getTopPatterns() { return topPatterns; }
    public StringShapeStats getStringShapeStats() { return stringShapeStats; }
    public SetFingerprint getSetFingerprint() { return setFingerprint; }
}
