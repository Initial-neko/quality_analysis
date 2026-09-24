package com.initialneko.qualityanalysis.regex;

import java.util.List;

/**
 * Decides which preset regex rule a column most plausibly belongs to, based on
 * the sampled values from the first N rows of the dataset (voting algorithm).
 */
public final class PresetSampleClassifier {
    private PresetSampleClassifier() {}

    /**
     * @param sampledValues sampled values of one column; null/blank entries are ignored
     * @param threshold minimum ratio (0..1] for a rule to become a candidate, e.g. 0.8
     * @return the best candidate (highest ratio; ties resolved by rule order), or null
     */
    public static Candidate classify(List<String> sampledValues, PresetRegexRegistry registry, double threshold) {
        if (sampledValues == null || sampledValues.isEmpty() || registry == null) return null;
        int total = 0;
        for (String value : sampledValues) {
            if (value != null && !value.trim().isEmpty()) total++;
        }
        if (total == 0) return null;

        Candidate best = null;
        for (PresetRegexRule rule : registry.rules()) {
            int hits = 0;
            for (String value : sampledValues) {
                if (rule.matches(value)) hits++;
            }
            double ratio = (double) hits / (double) total;
            if (ratio >= threshold && (best == null || ratio > best.ratio)) {
                best = new Candidate(rule, ratio);
            }
        }
        return best;
    }

    /** A matched candidate rule plus the ratio it achieved on the sample. */
    public static final class Candidate {
        public final PresetRegexRule rule;
        public final double ratio;

        Candidate(PresetRegexRule rule, double ratio) {
            this.rule = rule;
            this.ratio = ratio;
        }
    }
}
