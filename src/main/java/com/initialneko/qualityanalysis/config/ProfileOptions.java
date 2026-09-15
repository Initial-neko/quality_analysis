package com.initialneko.qualityanalysis.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Controls database read behavior and optional profiling features.
 *
 * Defaults are intentionally minimal: keep the signals needed for quality analysis,
 * and leave pattern/shape/relationship discovery off until explicitly requested.
 */
public final class ProfileOptions {
    private final int fetchSize;
    private final int queryTimeoutSeconds;
    private final int enumPrintThreshold;
    private final int lowCardinalityTrackingLimit;
    private final int topValueLimit;
    private final int patternLimit;
    private final int minHashBins;
    private final int clobPreviewChars;
    private final boolean trimStrings;
    private final boolean distinctEnabled;
    private final boolean valueFrequencyEnabled;
    private final boolean rangeEnabled;
    private final boolean lengthEnabled;
    private final boolean patternProfileEnabled;
    private final boolean stringShapeEnabled;
    private final boolean caseVariantTrackingEnabled;
    private final boolean relationshipFingerprintEnabled;
    private final Set<String> semanticNullTokens;

    private ProfileOptions(Builder b) {
        this.fetchSize = b.fetchSize;
        this.queryTimeoutSeconds = b.queryTimeoutSeconds;
        this.enumPrintThreshold = b.enumPrintThreshold;
        this.lowCardinalityTrackingLimit = b.lowCardinalityTrackingLimit;
        this.topValueLimit = b.topValueLimit;
        this.patternLimit = b.patternLimit;
        this.minHashBins = b.minHashBins;
        this.clobPreviewChars = b.clobPreviewChars;
        this.trimStrings = b.trimStrings;
        this.distinctEnabled = b.distinctEnabled;
        this.valueFrequencyEnabled = b.valueFrequencyEnabled;
        this.rangeEnabled = b.rangeEnabled;
        this.lengthEnabled = b.lengthEnabled;
        this.patternProfileEnabled = b.patternProfileEnabled;
        this.stringShapeEnabled = b.stringShapeEnabled;
        this.caseVariantTrackingEnabled = b.caseVariantTrackingEnabled;
        this.relationshipFingerprintEnabled = b.relationshipFingerprintEnabled;
        this.semanticNullTokens = Collections.unmodifiableSet(new LinkedHashSet<String>(b.semanticNullTokens));
    }

    public static Builder builder() { return new Builder(); }

    /** Minimal, production-oriented defaults. */
    public static ProfileOptions defaults() { return builder().build(); }

    /** Alias that makes the default intent explicit at call sites. */
    public static ProfileOptions minimal() { return defaults(); }

    public int getFetchSize() { return fetchSize; }
    public int getQueryTimeoutSeconds() { return queryTimeoutSeconds; }
    public int getEnumPrintThreshold() { return enumPrintThreshold; }
    public int getLowCardinalityTrackingLimit() { return lowCardinalityTrackingLimit; }
    public int getTopValueLimit() { return topValueLimit; }
    public int getPatternLimit() { return patternLimit; }
    public int getMinHashBins() { return minHashBins; }
    public int getClobPreviewChars() { return clobPreviewChars; }
    public boolean isTrimStrings() { return trimStrings; }
    public boolean isDistinctEnabled() { return distinctEnabled; }
    public boolean isValueFrequencyEnabled() { return valueFrequencyEnabled; }
    public boolean isRangeEnabled() { return rangeEnabled; }
    public boolean isLengthEnabled() { return lengthEnabled; }
    public boolean isPatternProfileEnabled() { return patternProfileEnabled; }
    public boolean isStringShapeEnabled() { return stringShapeEnabled; }
    public boolean isCaseVariantTrackingEnabled() { return caseVariantTrackingEnabled; }
    public boolean isRelationshipFingerprintEnabled() { return relationshipFingerprintEnabled; }
    public Set<String> getSemanticNullTokens() { return semanticNullTokens; }

    public boolean isSemanticNull(String value) {
        if (value == null) return false;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return semanticNullTokens.contains(normalized);
    }

    public static final class Builder {
        private int fetchSize = 10_000;
        private int queryTimeoutSeconds = 0;
        private int enumPrintThreshold = 20;
        private int lowCardinalityTrackingLimit = 100;
        private int topValueLimit = 20;
        private int patternLimit = 128;
        private int minHashBins = 32;
        private int clobPreviewChars = 0;
        private boolean trimStrings = true;

        // Core, low-cost signals.
        private boolean distinctEnabled = true;
        private boolean valueFrequencyEnabled = true;
        private boolean rangeEnabled = true;
        private boolean lengthEnabled = true;

        // Optional enrichment. Off by default.
        private boolean patternProfileEnabled = false;
        private boolean stringShapeEnabled = false;
        private boolean caseVariantTrackingEnabled = false;
        private boolean relationshipFingerprintEnabled = false;

        private Set<String> semanticNullTokens = new LinkedHashSet<String>(Arrays.asList("NULL", "N/A", "NA"));

        public Builder fetchSize(int v) { this.fetchSize = positive(v, "fetchSize"); return this; }
        public Builder queryTimeoutSeconds(int v) { if (v < 0) throw new IllegalArgumentException("queryTimeoutSeconds < 0"); this.queryTimeoutSeconds = v; return this; }
        public Builder enumPrintThreshold(int v) { this.enumPrintThreshold = positive(v, "enumPrintThreshold"); return this; }
        public Builder lowCardinalityTrackingLimit(int v) { this.lowCardinalityTrackingLimit = positive(v, "lowCardinalityTrackingLimit"); return this; }
        public Builder topValueLimit(int v) { this.topValueLimit = positive(v, "topValueLimit"); return this; }
        public Builder patternLimit(int v) { this.patternLimit = positive(v, "patternLimit"); return this; }
        public Builder minHashBins(int v) { this.minHashBins = positive(v, "minHashBins"); return this; }
        public Builder clobPreviewChars(int v) { if (v < 0) throw new IllegalArgumentException("clobPreviewChars < 0"); this.clobPreviewChars = v; return this; }
        public Builder trimStrings(boolean v) { this.trimStrings = v; return this; }
        public Builder distinctEnabled(boolean v) { this.distinctEnabled = v; return this; }
        public Builder valueFrequencyEnabled(boolean v) { this.valueFrequencyEnabled = v; return this; }
        public Builder rangeEnabled(boolean v) { this.rangeEnabled = v; return this; }
        public Builder lengthEnabled(boolean v) { this.lengthEnabled = v; return this; }
        public Builder patternProfileEnabled(boolean v) { this.patternProfileEnabled = v; return this; }
        public Builder stringShapeEnabled(boolean v) { this.stringShapeEnabled = v; return this; }
        public Builder caseVariantTrackingEnabled(boolean v) { this.caseVariantTrackingEnabled = v; return this; }
        public Builder relationshipFingerprintEnabled(boolean v) { this.relationshipFingerprintEnabled = v; return this; }

        public Builder semanticNullTokens(Set<String> values) {
            LinkedHashSet<String> copy = new LinkedHashSet<String>();
            if (values != null) {
                for (String v : values) if (v != null) copy.add(v.trim().toUpperCase(Locale.ROOT));
            }
            this.semanticNullTokens = copy;
            return this;
        }

        public ProfileOptions build() {
            if (lowCardinalityTrackingLimit < enumPrintThreshold) {
                throw new IllegalArgumentException("lowCardinalityTrackingLimit must be >= enumPrintThreshold");
            }
            if (valueFrequencyEnabled && !distinctEnabled) {
                throw new IllegalArgumentException("valueFrequencyEnabled requires distinctEnabled");
            }
            if (relationshipFingerprintEnabled && !distinctEnabled) {
                throw new IllegalArgumentException("relationshipFingerprintEnabled requires distinctEnabled");
            }
            return new ProfileOptions(this);
        }

        private static int positive(int v, String name) {
            if (v <= 0) throw new IllegalArgumentException(name + " must be > 0");
            return v;
        }
    }
}
