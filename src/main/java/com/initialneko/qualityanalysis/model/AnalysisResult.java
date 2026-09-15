package com.initialneko.qualityanalysis.model;

import com.initialneko.qualityanalysis.rule.RuleResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Profiling output plus configured validation results from one table scan. */
public final class AnalysisResult {
    private final TableProfile profile;
    private final List<RuleResult> ruleResults;

    public AnalysisResult(TableProfile profile, List<RuleResult> ruleResults) {
        if (profile == null) throw new IllegalArgumentException("profile is null");
        this.profile = profile;
        this.ruleResults = ruleResults == null
                ? Collections.<RuleResult>emptyList()
                : Collections.unmodifiableList(new ArrayList<RuleResult>(ruleResults));
    }

    public TableProfile getProfile() { return profile; }
    public List<RuleResult> getRuleResults() { return ruleResults; }

    public boolean allRulesPassed() {
        for (RuleResult result : ruleResults) if (!result.isPassed()) return false;
        return true;
    }
}
