package com.initialneko.qualityanalysis.rule;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.jdbc.LobValue;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.util.ValueNormalizer;

import java.util.ArrayList;
import java.util.List;

/** Convenience base class for custom rules. */
public abstract class AbstractColumnRule implements ColumnRule {
    private final String id;
    private final int maxInvalidSamples;
    protected ColumnMetadata metadata;
    protected ProfileOptions options;
    protected long checkedCount;
    protected long invalidCount;
    protected final List<String> invalidSamples = new ArrayList<String>();

    protected AbstractColumnRule(String id) {
        this(id, 20);
    }

    protected AbstractColumnRule(String id, int maxInvalidSamples) {
        if (id == null || id.trim().length() == 0) throw new IllegalArgumentException("rule id is blank");
        if (maxInvalidSamples < 0) throw new IllegalArgumentException("maxInvalidSamples < 0");
        this.id = id;
        this.maxInvalidSamples = maxInvalidSamples;
    }

    @Override
    public final String getId() { return id; }

    @Override
    public void start(ColumnMetadata metadata, ProfileOptions options) {
        this.metadata = metadata;
        this.options = options;
        this.checkedCount = 0L;
        this.invalidCount = 0L;
        this.invalidSamples.clear();
        onStart();
    }

    protected void onStart() { }

    protected final String canonical(Object value) {
        return ValueNormalizer.canonical(value, options);
    }

    /** True for physical null, blank strings, semantic-null tokens and LOB placeholders. */
    protected final boolean isMissing(Object value) {
        if (value == null) return true;
        if (value instanceof LobValue) return true;
        String normalized = canonical(value);
        return normalized == null || normalized.length() == 0 || options.isSemanticNull(normalized);
    }

    protected final void checked() { checkedCount++; }

    protected final void invalid(Object value) {
        invalidCount++;
        if (invalidSamples.size() < maxInvalidSamples) invalidSamples.add(display(value));
    }

    protected final String display(Object value) {
        if (value == null) return "<NULL>";
        if (value instanceof LobValue) return "<LOB>";
        String normalized = canonical(value);
        return normalized == null ? String.valueOf(value) : normalized;
    }

    protected final RuleResult result(boolean passed, String message) {
        return new RuleResult(metadata.getName(), id, passed, checkedCount, invalidCount, message, invalidSamples);
    }
}
