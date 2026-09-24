package com.initialneko.qualityanalysis.regex;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class PresetSampleClassifierTest {
    @Test
    public void picksBestCandidateAboveThreshold() {
        // 10 个非空样本，9 个手机号、1 个噪声 → 手机号 ratio 0.9
        PresetSampleClassifier.Candidate candidate = PresetSampleClassifier.classify(
                Arrays.asList("13812345678", "13912345678", "15012345678", "18612345678",
                        "17112345678", "13012345678", "15512345678", "18812345678",
                        "19912345678", "noise"),
                PresetRegexRegistry.withBuiltins(), 0.8d);
        assertNotNull(candidate);
        assertEquals("手机号", candidate.rule.getName());
        assertEquals(0.9d, candidate.ratio, 0.000001d);
    }

    @Test
    public void returnsNullWhenNothingReachesThreshold() {
        assertNull(PresetSampleClassifier.classify(
                Arrays.asList("abc", "def", "13812345678"),
                PresetRegexRegistry.withBuiltins(), 0.8d));
    }

    @Test
    public void returnsNullForEmptySample() {
        assertNull(PresetSampleClassifier.classify(Collections.<String>emptyList(),
                PresetRegexRegistry.withBuiltins(), 0.8d));
        assertNull(PresetSampleClassifier.classify(Arrays.asList((String) null, "  "),
                PresetRegexRegistry.withBuiltins(), 0.8d));
    }

    @Test
    public void tiesResolveByRegistryOrder() {
        // "123456" 同时命中 整数 与 邮编，ratio 均为 1.0；注册表顺序中 整数 在前
        PresetSampleClassifier.Candidate candidate = PresetSampleClassifier.classify(
                Collections.singletonList("123456"),
                PresetRegexRegistry.withBuiltins(), 0.8d);
        assertNotNull(candidate);
        assertEquals("整数", candidate.rule.getName());
    }

    @Test
    public void allMatchYieldsPerfectRatio() {
        PresetSampleClassifier.Candidate candidate = PresetSampleClassifier.classify(
                Arrays.asList("a@b.com", "c@d.cn"),
                PresetRegexRegistry.withBuiltins(), 0.8d);
        assertNotNull(candidate);
        assertEquals("邮箱", candidate.rule.getName());
        assertEquals(1.0d, candidate.ratio, 0.000001d);
    }
}
