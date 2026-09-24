package com.initialneko.qualityanalysis.regex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Built-in preset rules for common Chinese business data types.
 * Order matters: when two rules tie on match ratio the earlier one wins.
 */
public final class BuiltinPresetRules {
    private BuiltinPresetRules() {}

    public static List<PresetRegexRule> builtins() {
        List<PresetRegexRule> rules = new ArrayList<PresetRegexRule>();
        rules.add(new PresetRegexRule("手机号", "中国大陆11位手机号", "1[3-9]\\d{9}", false, 0));
        rules.add(new PresetRegexRule("身份证", "18位居民身份证号",
                "[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]", false, 0));
        rules.add(new PresetRegexRule("邮箱", "电子邮件地址",
                "[A-Za-z0-9+_.\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}", false, 0));
        rules.add(new PresetRegexRule("日期时间", "yyyy-MM-dd 或带时间的日期时间",
                "\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}(:\\d{2})?", false, 0));
        rules.add(new PresetRegexRule("日期", "yyyy-MM-dd 日期", "\\d{4}-\\d{2}-\\d{2}", false, 0));
        rules.add(new PresetRegexRule("URL", "http/https 链接", "https?://\\S+", false, 0));
        rules.add(new PresetRegexRule("整数", "十进制整数", "-?\\d+", false, 0));
        rules.add(new PresetRegexRule("小数", "十进制小数", "-?\\d+\\.\\d+", false, 0));
        rules.add(new PresetRegexRule("邮编", "6位邮政编码", "[1-9]\\d{5}", false, 0));
        return Collections.unmodifiableList(rules);
    }

    /** Convenience view used by tests and docs. */
    public static List<String> names() {
        return Arrays.asList("手机号", "身份证", "邮箱", "日期时间", "日期", "URL", "整数", "小数", "邮编");
    }
}
