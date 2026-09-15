package com.initialneko.qualityanalysis.model;

public final class StringShapeStats {
    private final long numericOnly;
    private final long alphabeticOnly;
    private final long alphanumericOnly;
    private final long chineseOnly;
    private final long containsWhitespace;
    private final long containsSpecial;
    private final long mixed;

    public StringShapeStats(long numericOnly, long alphabeticOnly, long alphanumericOnly, long chineseOnly,
                            long containsWhitespace, long containsSpecial, long mixed) {
        this.numericOnly = numericOnly;
        this.alphabeticOnly = alphabeticOnly;
        this.alphanumericOnly = alphanumericOnly;
        this.chineseOnly = chineseOnly;
        this.containsWhitespace = containsWhitespace;
        this.containsSpecial = containsSpecial;
        this.mixed = mixed;
    }

    public long getNumericOnly() { return numericOnly; }
    public long getAlphabeticOnly() { return alphabeticOnly; }
    public long getAlphanumericOnly() { return alphanumericOnly; }
    public long getChineseOnly() { return chineseOnly; }
    public long getContainsWhitespace() { return containsWhitespace; }
    public long getContainsSpecial() { return containsSpecial; }
    public long getMixed() { return mixed; }
}
