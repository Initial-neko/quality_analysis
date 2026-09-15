package com.initialneko.qualityanalysis.rule;

public final class RuleBinding {
    private final String columnName;
    private final ColumnRule rule;

    public RuleBinding(String columnName, ColumnRule rule) {
        if (columnName == null || columnName.trim().length() == 0) throw new IllegalArgumentException("columnName is blank");
        if (rule == null) throw new IllegalArgumentException("rule is null");
        this.columnName = columnName;
        this.rule = rule;
    }

    public String getColumnName() { return columnName; }
    public ColumnRule getRule() { return rule; }
}
