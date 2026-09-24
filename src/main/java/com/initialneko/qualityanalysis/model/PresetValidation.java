package com.initialneko.qualityanalysis.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of preset regex validation for one column.
 * The match rate denominator counts only non-blank string values:
 * NULL, blank and semantic-null values are excluded, mirroring the plan decision.
 */
public final class PresetValidation {
    private final String presetTypeName;
    private final String presetDescription;
    private final long matchedCount;
    private final long unmatchedCount;
    private final double matchRate;
    private final List<String> unmatchedSamples;

    public PresetValidation(String presetTypeName, String presetDescription,
                            long matchedCount, long unmatchedCount, double matchRate,
                            List<String> unmatchedSamples) {
        this.presetTypeName = presetTypeName;
        this.presetDescription = presetDescription;
        this.matchedCount = matchedCount;
        this.unmatchedCount = unmatchedCount;
        this.matchRate = matchRate;
        this.unmatchedSamples = unmatchedSamples == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(unmatchedSamples));
    }

    public String getPresetTypeName() { return presetTypeName; }
    public String getPresetDescription() { return presetDescription; }
    public long getMatchedCount() { return matchedCount; }
    public long getUnmatchedCount() { return unmatchedCount; }
    public double getMatchRate() { return matchRate; }
    public List<String> getUnmatchedSamples() { return unmatchedSamples; }
}
