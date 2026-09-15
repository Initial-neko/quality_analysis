package com.initialneko.qualityanalysis.rule;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.TableMetadata;
import com.initialneko.qualityanalysis.model.TableProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Executes configured rules during the same scan used by the profiler. */
public final class RuleEngine {
    private final List<BoundRule>[] rulesByColumn;
    private final List<BoundRule> allRules;

    @SuppressWarnings("unchecked")
    public RuleEngine(TableMetadata metadata, ProfileOptions options, List<RuleBinding> bindings) {
        int columnCount = metadata.getColumns().size();
        this.rulesByColumn = (List<BoundRule>[]) new List<?>[columnCount];
        this.allRules = new ArrayList<BoundRule>();

        if (bindings == null) return;
        for (RuleBinding binding : bindings) {
            int index = findColumnIndex(metadata, binding.getColumnName());
            if (index < 0) throw new IllegalArgumentException("unknown column: " + binding.getColumnName());
            ColumnMetadata column = metadata.getColumns().get(index);
            ColumnRule rule = binding.getRule();
            rule.start(column, options);
            BoundRule bound = new BoundRule(index, column.getName(), rule);
            if (rulesByColumn[index] == null) rulesByColumn[index] = new ArrayList<BoundRule>();
            rulesByColumn[index].add(bound);
            allRules.add(bound);
        }
    }

    public void acceptCell(int zeroBasedColumnIndex, Object value) {
        List<BoundRule> rules = rulesByColumn[zeroBasedColumnIndex];
        if (rules == null) return;
        for (BoundRule rule : rules) rule.rule.accept(value);
    }

    public List<RuleResult> finish(TableProfile profile) {
        if (allRules.isEmpty()) return Collections.emptyList();
        List<RuleResult> results = new ArrayList<RuleResult>(allRules.size());
        for (BoundRule bound : allRules) {
            ColumnProfile columnProfile = profile.findColumn(bound.columnName);
            results.add(bound.rule.finish(columnProfile));
        }
        return results;
    }

    private static int findColumnIndex(TableMetadata metadata, String columnName) {
        for (int i = 0; i < metadata.getColumns().size(); i++) {
            if (metadata.getColumns().get(i).getName().equalsIgnoreCase(columnName)) return i;
        }
        return -1;
    }

    private static final class BoundRule {
        private final int columnIndex;
        private final String columnName;
        private final ColumnRule rule;

        private BoundRule(int columnIndex, String columnName, ColumnRule rule) {
            this.columnIndex = columnIndex;
            this.columnName = columnName;
            this.rule = rule;
        }
    }
}
