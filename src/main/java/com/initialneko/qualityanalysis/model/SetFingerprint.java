package com.initialneko.qualityanalysis.model;

import java.util.Arrays;

public final class SetFingerprint {
    private final long distinctCount;
    private final long hashSum64;
    private final long hashXor64;
    private final long secondHashSum64;
    private final long[] minHashBins;

    public SetFingerprint(long distinctCount, long hashSum64, long hashXor64, long secondHashSum64, long[] minHashBins) {
        this.distinctCount = distinctCount;
        this.hashSum64 = hashSum64;
        this.hashXor64 = hashXor64;
        this.secondHashSum64 = secondHashSum64;
        this.minHashBins = minHashBins == null ? new long[0] : minHashBins.clone();
    }

    public long getDistinctCount() { return distinctCount; }
    public long getHashSum64() { return hashSum64; }
    public long getHashXor64() { return hashXor64; }
    public long getSecondHashSum64() { return secondHashSum64; }
    public long[] getMinHashBins() { return minHashBins.clone(); }

    public boolean exactSetFingerprintEquals(SetFingerprint other) {
        return other != null
                && distinctCount == other.distinctCount
                && hashSum64 == other.hashSum64
                && hashXor64 == other.hashXor64
                && secondHashSum64 == other.secondHashSum64;
    }

    @Override
    public String toString() {
        return "SetFingerprint{" +
                "distinctCount=" + distinctCount +
                ", hashSum64=" + Long.toUnsignedString(hashSum64) +
                ", hashXor64=" + Long.toUnsignedString(hashXor64) +
                ", secondHashSum64=" + Long.toUnsignedString(secondHashSum64) +
                ", minHashBins=" + Arrays.toString(minHashBins) +
                '}';
    }
}
