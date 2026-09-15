package com.initialneko.qualityanalysis.rule;

import com.initialneko.qualityanalysis.model.ColumnProfile;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Validates that non-missing values belong to a configured dictionary. */
public final class DictionaryRule extends AbstractColumnRule {
    private final Set<String> configuredValues = new HashSet<String>();
    private final boolean caseSensitive;
    private Set<String> normalizedValues;

    public DictionaryRule(String id, Collection<String> allowedValues) {
        this(id, allowedValues, true);
    }

    public DictionaryRule(String id, Collection<String> allowedValues, boolean caseSensitive) {
        super(id);
        if (allowedValues == null || allowedValues.isEmpty()) throw new IllegalArgumentException("allowedValues is empty");
        for (String value : allowedValues) if (value != null) configuredValues.add(value);
        if (configuredValues.isEmpty()) throw new IllegalArgumentException("allowedValues contains no usable value");
        this.caseSensitive = caseSensitive;
    }

    @Override
    protected void onStart() {
        normalizedValues = new HashSet<String>();
        for (String value : configuredValues) {
            String normalized = options.isTrimStrings() ? value.trim() : value;
            normalizedValues.add(caseSensitive ? normalized : normalized.toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public void accept(Object value) {
        if (isMissing(value)) return;
        checked();
        String normalized = canonical(value);
        String key = caseSensitive ? normalized : normalized.toLowerCase(Locale.ROOT);
        if (!normalizedValues.contains(key)) invalid(value);
    }

    @Override
    public RuleResult finish(ColumnProfile profile) {
        return result(invalidCount == 0L, "dictionarySize=" + normalizedValues.size());
    }
}
