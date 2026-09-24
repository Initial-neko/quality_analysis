package com.initialneko.qualityanalysis.regex;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PresetRegexRegistryTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void builtinRulesMatchCommonChineseBusinessTypes() {
        PresetRegexRegistry registry = PresetRegexRegistry.withBuiltins();
        PresetRegexRule phone = registry.rule("手机号");
        assertNotNull(phone);
        assertTrue(phone.matches("13812345678"));
        assertFalse(phone.matches("23812345678"));
        assertFalse(phone.matches("138123456781"));

        PresetRegexRule idcard = registry.rule("身份证");
        assertTrue(idcard.matches("11010519491231002X"));
        assertFalse(idcard.matches("1101051949123100"));

        PresetRegexRule email = registry.rule("邮箱");
        assertTrue(email.matches("a.b-c@test-domain.com.cn"));
        assertFalse(email.matches("not-an-email"));

        PresetRegexRule date = registry.rule("日期");
        assertTrue(date.matches("2026-09-24"));
        assertFalse(date.matches("2026-09-24T10:00"));
        assertTrue(registry.rule("日期时间").matches("2026-09-24T10:00"));

        PresetRegexRule integer = registry.rule("整数");
        assertTrue(integer.matches("-42"));
        assertFalse(integer.matches("3.14"));

        // 长度上限保护：超长输入不参与匹配
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 2000; i++) huge.append('1');
        assertFalse(phone.matches(huge.toString()));
    }

    @Test
    public void customRulesOverrideByNameAndAppendNewNames() {
        PresetRegexRule builtinPhone = PresetRegexRegistry.withBuiltins().rule("手机号");
        PresetRegexRule customPhone = new PresetRegexRule("手机号", "自定义手机号", "9\\d{10}", false, 0);
        PresetRegexRule brandNew = new PresetRegexRule("会员号", "VIP编号", "VIP\\d{8}", false, 0);
        PresetRegexRegistry merged = PresetRegexRegistry.withBuiltins()
                .mergeWith(Arrays.asList(customPhone, brandNew));

        assertEquals("自定义手机号", merged.rule("手机号").getDescription());
        assertTrue(merged.rule("手机号").matches("91381234567"));
        assertFalse(merged.rule("手机号").matches(builtinPhone.getPattern().pattern() == null ? "" : "13812345678"));
        assertNotNull(merged.rule("会员号"));
        assertEquals(BuiltinPresetRules.names().size() + 1, merged.rules().size());
    }

    @Test
    public void loaderReadsJsonConfigAndSkipsInvalidPatterns() throws Exception {
        Path config = temporaryFolder.newFile("preset-regex.json").toPath();
        String json = "{"
                + "\"rules\": ["
                + "  {\"name\": \"会员号\", \"description\": \"VIP编号\", \"pattern\": \"VIP\\\\d{8}\"},"
                + "  {\"name\": \"坏的\", \"description\": \"非法正则\", \"pattern\": \"([unclosed\"},"
                + "  {\"name\": \"大小写\", \"pattern\": \"abc\", \"caseInsensitive\": true}"
                + "]"
                + "}";
        Files.write(config, json.getBytes(StandardCharsets.UTF_8));

        List<PresetRegexRule> loaded = new PresetRegexLoader().load(config);
        assertEquals(2, loaded.size());
        assertEquals("会员号", loaded.get(0).getName());
        assertTrue(loaded.get(0).matches("VIP12345678"));
        assertTrue("caseInsensitive 应生效", loaded.get(1).matches("ABC"));
    }

    @Test
    public void duplicateBuiltinNameIsRejected() {
        PresetRegexRule duplicate = new PresetRegexRule("手机号", "dup", "1\\d{10}", false, 0);
        try {
            new PresetRegexRegistry(Arrays.asList(
                    BuiltinPresetRules.builtins().get(0), duplicate));
            fail("duplicate rule name must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        assertTrue(BuiltinPresetRules.builtins().get(0).matches("13812345678"));
        assertNotNull(Collections.emptyList());
    }
}
