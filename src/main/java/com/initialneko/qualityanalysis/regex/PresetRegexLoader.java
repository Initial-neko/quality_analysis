package com.initialneko.qualityanalysis.regex;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads user-defined preset regex rules from a JSON config file, using the
 * project's existing Gson dependency (no new third-party libraries).
 *
 * Expected format (see config/preset-regex.json.example):
 * <pre>
 * {
 *   "rules": [
 *     { "name": "会员号", "description": "8位会员编号", "pattern": "VIP\\d{8}" }
 *   ]
 * }
 * </pre>
 * Rules that fail to compile are skipped with a warning instead of aborting the run.
 */
public final class PresetRegexLoader {
    private static final Gson GSON = new Gson();

    public List<PresetRegexRule> load(Path configFile) throws IOException {
        Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8);
        try {
            return load(reader);
        } finally {
            reader.close();
        }
    }

    public List<PresetRegexRule> load(Reader reader) throws IOException {
        PresetRegexConfig config;
        try {
            config = GSON.fromJson(reader, PresetRegexConfig.class);
        } catch (JsonSyntaxException e) {
            throw new IOException("preset regex config is not valid JSON: " + e.getMessage(), e);
        }
        if (config == null || config.rules == null) return Collections.emptyList();
        List<PresetRegexRule> rules = new ArrayList<PresetRegexRule>();
        for (PresetRuleConfig item : config.rules) {
            if (item == null || item.name == null || item.pattern == null) {
                warn("skipping preset rule with missing name or pattern");
                continue;
            }
            try {
                rules.add(new PresetRegexRule(item.name, item.description, item.pattern,
                        item.caseInsensitive, item.maxInputLength));
            } catch (RuntimeException compileFailure) {
                warn("skipping invalid preset rule '" + item.name + "': " + compileFailure.getMessage());
            }
        }
        return Collections.unmodifiableList(rules);
    }

    private static void warn(String message) {
        System.err.println("[preset-regex] " + message);
    }

    static final class PresetRegexConfig {
        List<PresetRuleConfig> rules;
    }

    static final class PresetRuleConfig {
        @SerializedName("name") String name;
        @SerializedName("description") String description;
        @SerializedName("pattern") String pattern;
        @SerializedName("caseInsensitive") boolean caseInsensitive;
        @SerializedName("maxInputLength") int maxInputLength;
    }
}
