package com.initialneko.qualityanalysis.rule;

import com.initialneko.qualityanalysis.model.ColumnProfile;

/**
 * Validates column uniqueness by reusing the profiler's distinct accumulator.
 * It intentionally keeps no second HashSet.
 */
public final class UniqueRule extends AbstractColumnRule {
    private final double minUniqueness;

    public UniqueRule(String id) {
        this(id, 1.0d);
    }

    public UniqueRule(String id, double minUniqueness) {
        super(id, 0);
        if (minUniqueness < 0.0d || minUniqueness > 1.0d) {
            throw new IllegalArgumentException("minUniqueness must be between 0 and 1");
        }
        this.minUniqueness = minUniqueness;
    }

    @Override
    protected void onStart() {
        if (!options.isDistinctEnabled()) {
            throw new IllegalArgumentException("UniqueRule requires ProfileOptions.distinctEnabled(true)");
        }
    }

    @Override
    public void accept(Object value) {
        // No-op by design. Distinct state is already maintained by the profiler.
    }

    @Override
    public RuleResult finish(ColumnProfile profile) {
        checkedCount = profile.getNonNullCount();
        invalidCount = Math.max(0L, profile.getNonNullCount() - profile.getDistinctCount());
        double uniqueness = profile.getUniqueness();
        return result(uniqueness >= minUniqueness,
                "uniqueness=" + uniqueness + ", minRequired=" + minUniqueness);
    }
}
