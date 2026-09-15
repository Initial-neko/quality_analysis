package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.TableMetadata;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.ValueFamily;
import com.initialneko.qualityanalysis.profile.ProfileEngine;
import com.initialneko.qualityanalysis.rule.DictionaryRule;
import com.initialneko.qualityanalysis.rule.NullRateRule;
import com.initialneko.qualityanalysis.rule.RangeRule;
import com.initialneko.qualityanalysis.rule.RegexRule;
import com.initialneko.qualityanalysis.rule.RuleBinding;
import com.initialneko.qualityanalysis.rule.RuleEngine;
import com.initialneko.qualityanalysis.rule.RuleResult;
import com.initialneko.qualityanalysis.rule.UniqueRule;
import org.junit.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class RuleEngineTest {
    @Test
    public void validatesFiveBuiltInRulesInSamePass() {
        TableMetadata metadata = new TableMetadata("TEST", "PERSON", Arrays.asList(
                col(1, "ID", Types.BIGINT, ValueFamily.NUMBER),
                col(2, "GENDER", Types.VARCHAR, ValueFamily.STRING),
                col(3, "AGE", Types.INTEGER, ValueFamily.NUMBER),
                col(4, "PHONE", Types.VARCHAR, ValueFamily.STRING),
                col(5, "OPTIONAL_CODE", Types.VARCHAR, ValueFamily.STRING)
        ));
        ProfileOptions options = ProfileOptions.defaults();
        List<RuleBinding> bindings = Arrays.asList(
                new RuleBinding("ID", new UniqueRule("id-unique")),
                new RuleBinding("GENDER", new DictionaryRule("gender-dict", Arrays.asList("M", "F", "U"))),
                new RuleBinding("AGE", new RangeRule("age-range", "0", "120")),
                new RuleBinding("PHONE", new RegexRule("phone-regex", "^1[3-9]\\d{9}$")),
                new RuleBinding("OPTIONAL_CODE", new NullRateRule("optional-null", 0.20d))
        );

        ProfileEngine profiler = new ProfileEngine(metadata, options);
        RuleEngine rules = new RuleEngine(metadata, options, bindings);
        Object[][] rows = new Object[][] {
                {1L, "M", 20, "13800000001", "A"},
                {2L, "F", 130, "13800000002", null},
                {2L, "X", 30, "138-0000-0003", "B"},
                {3L, "U", 40, "13900000004", "NULL"},
                {4L, "F", 50, "13700000005", "C"}
        };

        for (Object[] row : rows) {
            profiler.beginRow();
            for (int i = 0; i < row.length; i++) {
                profiler.acceptCell(i, row[i]);
                rules.acceptCell(i, row[i]);
            }
            profiler.endRow();
        }

        TableProfile profile = profiler.finish();
        List<RuleResult> results = rules.finish(profile);
        assertEquals(5, results.size());
        assertResult(results, "id-unique", 1L);
        assertResult(results, "gender-dict", 1L);
        assertResult(results, "age-range", 1L);
        assertResult(results, "phone-regex", 1L);
        assertResult(results, "optional-null", 2L);

        RuleResult regex = find(results, "phone-regex");
        assertEquals("PHONE", regex.getColumnName());
        assertEquals(1, regex.getInvalidSamples().size());
        assertEquals("138-0000-0003", regex.getInvalidSamples().get(0));
    }

    private static void assertResult(List<RuleResult> results, String id, long invalidCount) {
        RuleResult result = find(results, id);
        assertNotNull(result);
        assertFalse(result.isPassed());
        assertEquals(invalidCount, result.getInvalidCount());
    }

    private static RuleResult find(List<RuleResult> results, String id) {
        for (RuleResult result : results) if (id.equals(result.getRuleId())) return result;
        return null;
    }

    private static ColumnMetadata col(int ordinal, String name, int jdbcType, ValueFamily family) {
        return new ColumnMetadata(ordinal, name, name, jdbcType, name, 0, 0, true, false, family);
    }
}
