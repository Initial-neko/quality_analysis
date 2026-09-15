package com.initialneko.qualityanalysis.rule;

import com.initialneko.qualityanalysis.model.ColumnProfile;

import java.util.regex.Pattern;

/** Validates every non-missing value against one Java regular expression. */
public final class RegexRule extends AbstractColumnRule {
    private final String expression;
    private final Pattern pattern;

    public RegexRule(String id, String expression) {
        super(id);
        if (expression == null || expression.length() == 0) throw new IllegalArgumentException("regex is blank");
        this.expression = expression;
        this.pattern = Pattern.compile(expression);
    }

    @Override
    public void accept(Object value) {
        if (isMissing(value)) return;
        checked();
        String normalized = canonical(value);
        if (!pattern.matcher(normalized).matches()) invalid(value);
    }

    @Override
    public RuleResult finish(ColumnProfile profile) {
        return result(invalidCount == 0L, "regex=" + expression);
    }
}
