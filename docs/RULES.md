# Rules and custom-rule extension

## Rule model

A rule is attached to one physical column with `RuleBinding` and participates in the same table scan as automatic profiling.

```java
new RuleBinding("PHONE", new RegexRule("phone-format", "^1[3-9]\\d{9}$"))
```

The lifecycle is:

```java
public interface ColumnRule {
    String getId();
    void start(ColumnMetadata metadata, ProfileOptions options);
    void accept(Object value);
    RuleResult finish(ColumnProfile profile);
}
```

A rule instance is **stateful and single-scan**. Do not reuse the same instance concurrently for multiple columns/tables.

## Built-in rules

### NullRateRule

```java
new NullRateRule("required", 0.0d)
new NullRateRule("optional", 0.05d, true, true)
```

The last two flags mean whether blank strings and configured semantic-null tokens (`NULL`, `N/A`, `NA` by default) count as missing.

### DictionaryRule

```java
new DictionaryRule("gender", Arrays.asList("M", "F", "U"))
```

Default comparison is case-sensitive after the configured string trimming policy. An overload supports case-insensitive comparison.

Physical/blank/semantic missing values are skipped; combine with `NullRateRule` when missing values should fail.

### RangeRule

```java
new RangeRule("age", "0", "120")
new RangeRule("business-date", "2000-01-01", "2099-12-31")
```

V1 supports `NUMBER` and `DATE_TIME` columns. Bounds are inclusive. Date bounds use ISO `yyyy-MM-dd`; timestamp values are validated by their date part.

### RegexRule

```java
new RegexRule("mobile", "^1[3-9]\\d{9}$")
new RegexRule("id-shape", "^\\d{17}[0-9Xx]$")
```

`RegexRule` uses Java `Pattern.matcher(value).matches()`, so the complete normalized value must match.

Regex is only a format validator. For a Chinese ID number, checksum/birth-date/administrative-code semantics belong in a custom rule.

### UniqueRule

```java
new UniqueRule("order-id-unique")
new UniqueRule("business-key-near-unique", 0.999d)
```

`UniqueRule` does **not** allocate another `HashSet`. It reads the final exact distinct/uniqueness already calculated by `ColumnProfile`, therefore `ProfileOptions.distinctEnabled(true)` is required.

## Running rules

```java
ProfileOptions options = ProfileOptions.defaults();

List<RuleBinding> rules = Arrays.asList(
        new RuleBinding("CUSTOMER_ID", new UniqueRule("customer-id-unique")),
        new RuleBinding("GENDER", new DictionaryRule(
                "gender-dict", Arrays.asList("M", "F", "U"))),
        new RuleBinding("AGE", new RangeRule("age-range", "0", "120")),
        new RuleBinding("PHONE", new RegexRule(
                "phone-format", "^1[3-9]\\d{9}$")),
        new RuleBinding("OPTIONAL_CODE", new NullRateRule(
                "optional-null", 0.05d))
);

AnalysisResult result = new QualityAnalyzer().analyzeTable(
        connection, "SCHEMA_NAME", "CUSTOMER", options, rules);

for (RuleResult rule : result.getRuleResults()) {
    System.out.println(rule.getColumnName()
            + " / " + rule.getRuleId()
            + " / passed=" + rule.isPassed()
            + " / invalid=" + rule.getInvalidCount()
            + " / samples=" + rule.getInvalidSamples());
}
```

## Adding a custom rule

Prefer extending `AbstractColumnRule`. It already provides:

- metadata and `ProfileOptions`
- canonical string conversion
- missing-value helper
- checked/invalid counters
- bounded invalid samples
- `RuleResult` construction

Example: a simple prefix rule.

```java
package com.example.quality;

import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.rule.AbstractColumnRule;
import com.initialneko.qualityanalysis.rule.RuleResult;

public final class PrefixRule extends AbstractColumnRule {
    private final String prefix;

    public PrefixRule(String id, String prefix) {
        super(id);
        if (prefix == null) throw new IllegalArgumentException("prefix is null");
        this.prefix = prefix;
    }

    @Override
    public void accept(Object value) {
        if (isMissing(value)) return;
        checked();
        if (!canonical(value).startsWith(prefix)) invalid(value);
    }

    @Override
    public RuleResult finish(ColumnProfile profile) {
        return result(invalidCount == 0L, "requiredPrefix=" + prefix);
    }
}
```

Use it without modifying the engine:

```java
new RuleBinding("ORG_CODE", new PrefixRule("org-prefix", "ORG-"))
```

## Profile-backed custom rules

If the profile already calculated what the rule needs, avoid re-accumulating it. `UniqueRule` is the reference pattern:

```java
@Override
public void accept(Object value) {
    // no-op
}

@Override
public RuleResult finish(ColumnProfile profile) {
    // evaluate profile.getDistinctCount(), getUniqueness(), getNullRate(), ...
}
```

This is preferred for future thresholds based on existing aggregates.

## Custom-rule constraints

A production custom rule should follow these constraints:

1. Never execute SQL or open another JDBC ResultSet.
2. Never retain complete rows or all raw values unless the requirement explicitly demands it.
3. Keep sample lists bounded.
4. Reuse `ColumnProfile` aggregates whenever possible.
5. Treat CLOB/BLOB deliberately; do not materialize them accidentally.
6. Keep null policy separate unless nullness is the rule itself.
7. Add deterministic mock data and assertions for every new rule.
8. Use one new rule class for one business meaning; do not build a generic scripting/DSL engine until there is a concrete requirement.

## Suggested future custom rules

Only add these when required by a real standard:

- Chinese ID checksum/date/region validation
- organization-code checksum
- precision/scale limits
- cross-field consistency rules (these require a separate row-level rule interface, not hacks inside `ColumnRule`)

Cross-field validation is intentionally not part of V1 because `ColumnRule` has a single-column contract.
