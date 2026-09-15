package com.initialneko.qualityanalysis.model;

public final class ValueFrequency {
    private final String value;
    private final long count;
    private final double ratio;

    public ValueFrequency(String value, long count, double ratio) {
        this.value = value;
        this.count = count;
        this.ratio = ratio;
    }

    public String getValue() { return value; }
    public long getCount() { return count; }
    public double getRatio() { return ratio; }
}
