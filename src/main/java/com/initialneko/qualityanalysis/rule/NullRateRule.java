package com.initialneko.qualityanalysis.rule;

import com.initialneko.qualityanalysis.model.ColumnProfile;

/** Validates the maximum allowed missing-value rate. */
public final class NullRateRule extends AbstractColumnRule {
    private final double maxMissingRate;
    private final boolean includeBlank;
    private final boolean includeSemanticNull;

    public NullRateRule(String id, double maxMissingRate) {
        this(id, maxMissingRate, true, true);
    }

    public NullRateRule(String id, double maxMissingRate, boolean includeBlank, boolean includeSemanticNull) {
        super(id);
        if (maxMissingRate < 0.0d || maxMissingRate > 1.0d) {
            throw new IllegalArgumentException("maxMissingRate must be between 0 and 1");
        }
        this.maxMissingRate = maxMissingRate;
        this.includeBlank = includeBlank;
        this.includeSemanticNull = includeSemanticNull;
    }

    @Override
    public void accept(Object value) {
        checked();
        boolean missing = value == null;
        if (!missing && value instanceof String) {
            String normalized = canonical(value);
            if (includeBlank && normalized.length() == 0) missing = true;
            else if (includeSemanticNull && options.isSemanticNull(normalized)) missing = true;
        }
        if (missing) invalid(value);
    }

    @Override
    public RuleResult finish(ColumnProfile profile) {
        double rate = checkedCount == 0 ? 0.0d : (double) invalidCount / (double) checkedCount;
        boolean passed = rate <= maxMissingRate;
        return result(passed, "missingRate=" + rate + ", maxAllowed=" + maxMissingRate);
    }
}
