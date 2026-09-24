package com.initialneko.qualityanalysis.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable preset regex rule.
 * Matching is a full (anchored) match and input length is capped so that
 * pathological user-supplied patterns cannot run unbounded (ReDoS guard).
 */
public final class PresetRegexRule {
    public static final int DEFAULT_MAX_INPUT_LENGTH = 512;

    private final String name;
    private final String description;
    private final Pattern pattern;
    private final int maxInputLength;

    public PresetRegexRule(String name, String description, String regex,
                           boolean caseInsensitive, int maxInputLength) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("rule name must not be empty");
        if (regex == null || regex.isEmpty()) throw new IllegalArgumentException("rule regex must not be empty");
        this.name = name.trim();
        this.description = description == null ? "" : description;
        this.pattern = caseInsensitive
                ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
                : Pattern.compile(regex);
        this.maxInputLength = maxInputLength <= 0 ? DEFAULT_MAX_INPUT_LENGTH : maxInputLength;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Pattern getPattern() { return pattern; }
    public int getMaxInputLength() { return maxInputLength; }

    /** Full anchored match; null / over-length input never matches. */
    public boolean matches(String value) {
        if (value == null || value.length() > maxInputLength) return false;
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }

    @Override public String toString() { return name; }
}
