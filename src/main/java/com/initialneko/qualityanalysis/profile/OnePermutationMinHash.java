package com.initialneko.qualityanalysis.profile;

import com.initialneko.qualityanalysis.util.Hash64;

import java.util.Arrays;

final class OnePermutationMinHash {
    private final long[] bins;

    OnePermutationMinHash(int binCount) {
        this.bins = new long[binCount];
        Arrays.fill(this.bins, Long.MAX_VALUE);
    }

    void addHash(long hash) {
        int bin = (int) Math.floorMod(hash, bins.length);
        long rank = Hash64.mix64(hash ^ 0x9e3779b97f4a7c15L) & Long.MAX_VALUE;
        if (rank < bins[bin]) bins[bin] = rank;
    }

    long[] snapshot() { return bins.clone(); }
}
