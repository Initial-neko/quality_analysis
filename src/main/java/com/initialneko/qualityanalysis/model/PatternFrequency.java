package com.initialneko.qualityanalysis.model;

public final class PatternFrequency {
    private final String pattern;
    private final long count;

    public PatternFrequency(String pattern, long count) {
        this.pattern = pattern;
        this.count = count;
    }

    public String getPattern() { return pattern; }
    public long getCount() { return count; }
}
