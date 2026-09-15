package com.initialneko.qualityanalysis.rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RuleResult {
    private final String columnName;
    private final String ruleId;
    private final boolean passed;
    private final long checkedCount;
    private final long invalidCount;
    private final String message;
    private final List<String> invalidSamples;

    public RuleResult(String columnName, String ruleId, boolean passed,
                      long checkedCount, long invalidCount, String message,
                      List<String> invalidSamples) {
        this.columnName = columnName;
        this.ruleId = ruleId;
        this.passed = passed;
        this.checkedCount = checkedCount;
        this.invalidCount = invalidCount;
        this.message = message;
        this.invalidSamples = invalidSamples == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(invalidSamples));
    }

    public String getColumnName() { return columnName; }
    public String getRuleId() { return ruleId; }
    public boolean isPassed() { return passed; }
    public long getCheckedCount() { return checkedCount; }
    public long getInvalidCount() { return invalidCount; }
    public String getMessage() { return message; }
    public List<String> getInvalidSamples() { return invalidSamples; }

    public double getInvalidRate() {
        return checkedCount == 0 ? 0.0d : (double) invalidCount / (double) checkedCount;
    }

    public double getValidRate() {
        return checkedCount == 0 ? 1.0d : 1.0d - getInvalidRate();
    }
}
