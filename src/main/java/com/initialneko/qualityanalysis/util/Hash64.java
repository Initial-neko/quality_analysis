package com.initialneko.qualityanalysis.util;

import java.nio.charset.StandardCharsets;

public final class Hash64 {
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private Hash64() {}

    public static long hash(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        long h = FNV_OFFSET_BASIS;
        for (byte b : bytes) {
            h ^= (b & 0xff);
            h *= FNV_PRIME;
        }
        return mix64(h);
    }

    public static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }
}
