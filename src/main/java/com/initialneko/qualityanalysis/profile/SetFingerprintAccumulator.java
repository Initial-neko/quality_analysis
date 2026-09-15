package com.initialneko.qualityanalysis.profile;

import com.initialneko.qualityanalysis.model.SetFingerprint;
import com.initialneko.qualityanalysis.util.Hash64;

final class SetFingerprintAccumulator {
    private long distinctCount;
    private long hashSum;
    private long hashXor;
    private long secondHashSum;
    private final OnePermutationMinHash minHash;

    SetFingerprintAccumulator(int minHashBins) {
        this.minHash = new OnePermutationMinHash(minHashBins);
    }

    void addDistinct(String value) {
        long h = Hash64.hash(value);
        long h2 = Hash64.mix64(h ^ 0xd6e8feb86659fd93L);
        distinctCount++;
        hashSum += h;
        hashXor ^= h;
        secondHashSum += h2;
        minHash.addHash(h);
    }

    SetFingerprint finish() {
        return new SetFingerprint(distinctCount, hashSum, hashXor, secondHashSum, minHash.snapshot());
    }
}
