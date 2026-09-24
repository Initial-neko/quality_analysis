package com.initialneko.qualityanalysis.regex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable registry of preset rules: builtin defaults plus optional custom rules. */
public final class PresetRegexRegistry {
    private final List<PresetRegexRule> rules;
    private final Map<String, PresetRegexRule> byName;

    public PresetRegexRegistry(List<PresetRegexRule> rules) {
        if (rules == null) throw new IllegalArgumentException("rules must not be null");
        List<PresetRegexRule> copy = new ArrayList<PresetRegexRule>(rules);
        Map<String, PresetRegexRule> index = new HashMap<String, PresetRegexRule>();
        for (PresetRegexRule rule : copy) {
            PresetRegexRule previous = index.put(rule.getName(), rule);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate preset rule name: " + rule.getName());
            }
        }
        this.rules = Collections.unmodifiableList(copy);
        this.byName = Collections.unmodifiableMap(index);
    }

    public static PresetRegexRegistry withBuiltins() {
        return new PresetRegexRegistry(BuiltinPresetRules.builtins());
    }

    /** Custom rules override builtin rules with the same name; new names are appended in order. */
    public PresetRegexRegistry mergeWith(List<PresetRegexRule> customRules) {
        if (customRules == null || customRules.isEmpty()) return this;
        Map<String, PresetRegexRule> merged = new HashMap<String, PresetRegexRule>(byName);
        List<PresetRegexRule> appended = new ArrayList<PresetRegexRule>();
        for (PresetRegexRule custom : customRules) {
            if (merged.containsKey(custom.getName())) {
                merged.put(custom.getName(), custom);
            } else {
                merged.put(custom.getName(), custom);
                appended.add(custom);
            }
        }
        List<PresetRegexRule> result = new ArrayList<PresetRegexRule>();
        for (PresetRegexRule builtin : rules) {
            result.add(merged.get(builtin.getName()));
        }
        result.addAll(appended);
        return new PresetRegexRegistry(result);
    }

    public List<PresetRegexRule> rules() { return rules; }

    public PresetRegexRule rule(String name) { return byName.get(name); }
}
