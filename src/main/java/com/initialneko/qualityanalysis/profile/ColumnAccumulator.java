package com.initialneko.qualityanalysis.profile;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.jdbc.LobValue;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.PatternFrequency;
import com.initialneko.qualityanalysis.model.PresetValidation;
import com.initialneko.qualityanalysis.model.SetFingerprint;
import com.initialneko.qualityanalysis.model.StringShapeStats;
import com.initialneko.qualityanalysis.model.ValueFrequency;
import com.initialneko.qualityanalysis.model.ValueFamily;
import com.initialneko.qualityanalysis.regex.PresetRegexRule;
import com.initialneko.qualityanalysis.util.PatternFingerprint;
import com.initialneko.qualityanalysis.util.StringShapeClassifier;
import com.initialneko.qualityanalysis.util.ValueNormalizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ColumnAccumulator {
    private final ColumnMetadata metadata;
    private final ProfileOptions options;
    private final Set<String> distinct;
    private final SetFingerprintAccumulator fingerprint;
    private long rowCount;
    private long nullCount;
    private long blankCount;
    private long semanticNullCount;
    private long nonNullCount;
    private long profiledValueCount;
    private long trimChangedCount;
    private Long minLength;
    private Long maxLength;
    private long totalLength;
    private long lengthCount;
    private Object minComparable;
    private Object maxComparable;
    private String minValue;
    private String maxValue;
    private Map<String, Long> lowCardinalityFrequencies;
    private Map<String, Set<String>> foldedVariants;
    private final Map<String, Long> patternCounts;
    private long patternOverflowCount;
    private long numericOnly;
    private long alphabeticOnly;
    private long alphanumericOnly;
    private long chineseOnly;
    private long containsWhitespace;
    private long containsSpecial;
    private long mixed;
    private boolean lobContentSkipped;

    // Preset regex validation (assigned mid-scan after sample-based classification).
    private PresetRegexRule presetRule;
    private long presetMatched;
    private long presetUnmatched;
    private List<String> presetUnmatchedSamples;
    private static final int PRESET_SAMPLE_LIMIT = 20;

    ColumnAccumulator(ColumnMetadata metadata, ProfileOptions options) {
        this.metadata = metadata;
        this.options = options;
        this.distinct = options.isDistinctEnabled() ? new HashSet<String>() : null;
        this.fingerprint = options.isRelationshipFingerprintEnabled()
                ? new SetFingerprintAccumulator(options.getMinHashBins()) : null;
        this.lowCardinalityFrequencies = options.isValueFrequencyEnabled()
                ? new LinkedHashMap<String, Long>() : null;
        this.foldedVariants = options.isCaseVariantTrackingEnabled()
                ? new HashMap<String, Set<String>>() : null;
        this.patternCounts = options.isPatternProfileEnabled()
                ? new HashMap<String, Long>() : null;
    }

    /** Assigns the preset rule mid-scan (after the first N rows were classified). */
    void setPresetRule(PresetRegexRule rule) {
        this.presetRule = rule;
        this.presetUnmatchedSamples = rule == null ? null : new ArrayList<String>();
    }

    /** Replays one sampled value against the assigned preset rule (backfill after classification). */
    void recordPresetValue(Object value) {
        if (presetRule == null || value == null) return;
        String raw = value instanceof String ? (String) value
                : (metadata.getFamily() == ValueFamily.STRING ? String.valueOf(value) : null);
        if (raw == null) return;
        String normalized = options.isTrimStrings() ? raw.trim() : raw;
        trackPreset(normalized);
    }

    private void trackPreset(String normalized) {
        if (presetRule == null || normalized == null || normalized.length() == 0) return;
        if (options.isSemanticNull(normalized)) return;
        if (presetRule.matches(normalized)) {
            presetMatched++;
        } else {
            presetUnmatched++;
            if (presetUnmatchedSamples != null && presetUnmatchedSamples.size() < PRESET_SAMPLE_LIMIT) {
                presetUnmatchedSamples.add(normalized);
            }
        }
    }

    void accept(Object value) {
        rowCount++;
        if (value == null) {
            nullCount++;
            return;
        }
        nonNullCount++;
        if (value instanceof LobValue) {
            acceptLob((LobValue) value);
            return;
        }
        if (value instanceof String || metadata.getFamily() == ValueFamily.STRING) {
            if (!acceptString(String.valueOf(value))) return;
        } else if (value instanceof byte[] && options.isLengthEnabled()) {
            acceptLength(((byte[]) value).length);
        }

        profiledValueCount++;
        String canonical = ValueNormalizer.canonical(value, options);
        if (distinct != null && canonical != null) {
            boolean first = distinct.add(canonical);
            if (first && fingerprint != null) fingerprint.addDistinct(canonical);
        }
        trackFrequency(canonical);
        if (options.isRangeEnabled()) trackMinMax(value, canonical);
    }

    private void acceptLob(LobValue lob) {
        lobContentSkipped = true;
        if (options.isLengthEnabled()) acceptLength(lob.getLength());
        if (options.isPatternProfileEnabled()
                && lob.getKind() == LobValue.Kind.CLOB
                && lob.getPreview() != null
                && lob.getPreview().length() > 0) {
            trackPattern(lob.getPreview());
        }
    }

    /** Returns false when a blank/semantic-null string must stop before downstream profiling. */
    private boolean acceptString(String raw) {
        String normalized = options.isTrimStrings() ? raw.trim() : raw;
        if (!raw.equals(normalized)) trimChangedCount++;
        if (normalized.length() == 0) {
            blankCount++;
            return false;
        }
        if (options.isSemanticNull(normalized)) {
            semanticNullCount++;
            return false;
        }
        if (presetRule != null) trackPreset(normalized);
        if (options.isLengthEnabled()) acceptLength(raw.length());
        if (options.isPatternProfileEnabled()) trackPattern(normalized);
        if (options.isStringShapeEnabled()) trackShape(normalized);
        if (foldedVariants != null) {
            String folded = normalized.toLowerCase(Locale.ROOT);
            Set<String> variants = foldedVariants.get(folded);
            if (variants == null) {
                variants = new HashSet<String>();
                foldedVariants.put(folded, variants);
            }
            variants.add(normalized);
            if (foldedVariants.size() > options.getLowCardinalityTrackingLimit()) foldedVariants = null;
        }
        return true;
    }

    private void acceptLength(long length) {
        if (minLength == null || length < minLength.longValue()) minLength = length;
        if (maxLength == null || length > maxLength.longValue()) maxLength = length;
        totalLength += length;
        lengthCount++;
    }

    private void trackFrequency(String canonical) {
        if (canonical == null || lowCardinalityFrequencies == null) return;
        Long old = lowCardinalityFrequencies.get(canonical);
        if (old == null) {
            lowCardinalityFrequencies.put(canonical, 1L);
            if (lowCardinalityFrequencies.size() > options.getLowCardinalityTrackingLimit()) lowCardinalityFrequencies = null;
        } else {
            lowCardinalityFrequencies.put(canonical, old + 1L);
        }
    }

    private void trackPattern(String value) {
        if (value == null || patternCounts == null) return;
        String pattern = PatternFingerprint.of(value);
        Long old = patternCounts.get(pattern);
        if (old != null) patternCounts.put(pattern, old + 1L);
        else if (patternCounts.size() < options.getPatternLimit()) patternCounts.put(pattern, 1L);
        else patternOverflowCount++;
    }

    private void trackShape(String value) {
        StringShapeClassifier.Shape shape = StringShapeClassifier.classify(value);
        if (shape.numericOnly()) numericOnly++;
        if (shape.alphabeticOnly()) alphabeticOnly++;
        if (shape.alphanumericOnly()) alphanumericOnly++;
        if (shape.chineseOnly()) chineseOnly++;
        if (shape.whitespace) containsWhitespace++;
        if (shape.special) containsSpecial++;
        if (shape.mixed()) mixed++;
    }

    private void trackMinMax(Object value, String canonical) {
        Object comparable = ValueNormalizer.comparable(value);
        if (comparable == null) return;
        if (minComparable == null || ValueNormalizer.compareComparable(comparable, minComparable) < 0) {
            minComparable = comparable;
            minValue = canonical;
        }
        if (maxComparable == null || ValueNormalizer.compareComparable(comparable, maxComparable) > 0) {
            maxComparable = comparable;
            maxValue = canonical;
        }
    }

    ColumnProfile finish() {
        long distinctCount = distinct == null || (lobContentSkipped && metadata.getFamily() == ValueFamily.LOB)
                ? 0L : distinct.size();
        double uniqueness = distinct == null || profiledValueCount == 0 || (lobContentSkipped && metadata.getFamily() == ValueFamily.LOB)
                ? 0.0d : (double) distinctCount / (double) profiledValueCount;
        boolean candidatePk = distinct != null && !lobContentSkipped && rowCount > 0
                && nullCount == 0 && blankCount == 0 && semanticNullCount == 0
                && distinctCount == rowCount;
        boolean constant = distinct != null && distinctCount == 1 && profiledValueCount > 0;
        boolean lowCardinality = distinct != null && lowCardinalityFrequencies != null
                && distinctCount <= options.getLowCardinalityTrackingLimit();
        List<ValueFrequency> values = buildValues();
        boolean quasiConstant = !values.isEmpty() && profiledValueCount > 0 && values.get(0).getRatio() >= 0.99d;
        Double avgLength = lengthCount == 0 ? null : (double) totalLength / (double) lengthCount;
        int caseVariantGroups = 0;
        if (foldedVariants != null) {
            for (Set<String> variants : foldedVariants.values()) if (variants.size() > 1) caseVariantGroups++;
        }
        SetFingerprint setFingerprint = fingerprint == null
                ? new SetFingerprint(0L, 0L, 0L, 0L, new long[0])
                : fingerprint.finish();
        PresetValidation presetValidation = buildPresetValidation();
        return new ColumnProfile(metadata, rowCount, nullCount, blankCount, semanticNullCount, nonNullCount,
                distinctCount, uniqueness, minValue, maxValue, minLength, maxLength, avgLength,
                trimChangedCount, caseVariantGroups, candidatePk, constant, quasiConstant,
                lowCardinality, lobContentSkipped, values, buildPatterns(),
                new StringShapeStats(numericOnly, alphabeticOnly, alphanumericOnly, chineseOnly,
                        containsWhitespace, containsSpecial, mixed), setFingerprint, presetValidation);
    }

    /** Match rate counts only non-blank string values (NULL/blank/semantic-null excluded). */
    private PresetValidation buildPresetValidation() {
        if (presetRule == null) return null;
        long total = presetMatched + presetUnmatched;
        if (total == 0) return null;
        double rate = (double) presetMatched / (double) total;
        return new PresetValidation(presetRule.getName(), presetRule.getDescription(),
                presetMatched, presetUnmatched, rate,
                presetUnmatchedSamples == null ? Collections.<String>emptyList() : presetUnmatchedSamples);
    }

    private List<ValueFrequency> buildValues() {
        if (lowCardinalityFrequencies == null || lowCardinalityFrequencies.isEmpty() || distinct == null) {
            return Collections.emptyList();
        }
        List<Map.Entry<String, Long>> entries = new ArrayList<Map.Entry<String, Long>>(lowCardinalityFrequencies.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Long>>() {
            @Override public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                int byCount = Long.compare(b.getValue(), a.getValue());
                return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
            }
        });
        int limit = distinct.size() <= options.getEnumPrintThreshold()
                ? entries.size() : Math.min(options.getTopValueLimit(), entries.size());
        List<ValueFrequency> result = new ArrayList<ValueFrequency>(limit);
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Long> e = entries.get(i);
            double ratio = profiledValueCount == 0 ? 0.0d : (double) e.getValue() / (double) profiledValueCount;
            result.add(new ValueFrequency(e.getKey(), e.getValue(), ratio));
        }
        return result;
    }

    private List<PatternFrequency> buildPatterns() {
        if (patternCounts == null || patternCounts.isEmpty()) return Collections.emptyList();
        List<PatternFrequency> list = new ArrayList<PatternFrequency>();
        for (Map.Entry<String, Long> e : patternCounts.entrySet()) {
            list.add(new PatternFrequency(e.getKey(), e.getValue()));
        }
        if (patternOverflowCount > 0) list.add(new PatternFrequency("OTHER", patternOverflowCount));
        Collections.sort(list, new Comparator<PatternFrequency>() {
            @Override public int compare(PatternFrequency a, PatternFrequency b) {
                return Long.compare(b.getCount(), a.getCount());
            }
        });
        if (list.size() > options.getTopValueLimit()) {
            return new ArrayList<PatternFrequency>(list.subList(0, options.getTopValueLimit()));
        }
        return list;
    }
}
