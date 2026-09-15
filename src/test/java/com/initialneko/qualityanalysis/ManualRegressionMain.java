package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.RelationshipCandidate;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.relation.RelationshipAnalyzer;
import com.initialneko.qualityanalysis.rule.DictionaryRule;
import com.initialneko.qualityanalysis.rule.NullRateRule;
import com.initialneko.qualityanalysis.rule.RangeRule;
import com.initialneko.qualityanalysis.rule.RegexRule;
import com.initialneko.qualityanalysis.rule.UniqueRule;

import java.util.Arrays;
import java.util.List;

public final class ManualRegressionMain {
    private ManualRegressionMain() {}

    public static void main(String[] args) {
        TableProfile customers = MockDatasets.customers();
        check(customers.getRowCount() == 200L, "row count");
        check(customers.findColumn("CUSTOMER_ID").isCandidatePrimaryKey(), "candidate key");
        check(customers.findColumn("STATUS").getDistinctCount() == 5L, "status distinct");
        check(customers.findColumn("OPTIONAL_CODE").getNullCount() == 1L, "physical null");
        check(customers.findColumn("OPTIONAL_CODE").getBlankCount() == 1L, "blank");
        check(customers.findColumn("OPTIONAL_CODE").getSemanticNullCount() == 1L, "semantic null");
        check("-1".equals(customers.findColumn("AGE").getMinValue()), "numeric min");
        check("150".equals(customers.findColumn("AGE").getMaxValue()), "numeric max");
        ColumnProfile clob = customers.findColumn("NOTE");
        check(clob.isLobContentSkipped() && clob.getDistinctCount() == 0L, "clob strategy");

        ProfileOptions minimal = ProfileOptions.defaults();
        check(!minimal.isPatternProfileEnabled(), "pattern default off");
        check(!minimal.isRelationshipFingerprintEnabled(), "relationship default off");

        RegexRule regex = new RegexRule("phone", "^1[3-9]\\d{9}$");
        regex.start(customers.findColumn("PHONE").getMetadata(), minimal);
        regex.accept("13800000001");
        regex.accept("138-0000-0002");
        check(regex.finish(customers.findColumn("PHONE")).getInvalidCount() == 1L, "regex rule");

        DictionaryRule dictionary = new DictionaryRule("gender", Arrays.asList("M", "F", "U"));
        dictionary.start(customers.findColumn("GENDER").getMetadata(), minimal);
        dictionary.accept("M");
        dictionary.accept("X");
        check(dictionary.finish(customers.findColumn("GENDER")).getInvalidCount() == 1L, "dictionary rule");

        RangeRule range = new RangeRule("age", "0", "120");
        range.start(customers.findColumn("AGE").getMetadata(), minimal);
        range.accept(Integer.valueOf(20));
        range.accept(Integer.valueOf(130));
        check(range.finish(customers.findColumn("AGE")).getInvalidCount() == 1L, "range rule");

        NullRateRule nulls = new NullRateRule("null", 0.25d);
        nulls.start(customers.findColumn("OPTIONAL_CODE").getMetadata(), minimal);
        nulls.accept("A");
        nulls.accept(null);
        nulls.accept("NULL");
        check(nulls.finish(customers.findColumn("OPTIONAL_CODE")).getInvalidCount() == 2L, "null rule");

        UniqueRule unique = new UniqueRule("id-unique");
        unique.start(customers.findColumn("CUSTOMER_ID").getMetadata(), minimal);
        check(unique.finish(customers.findColumn("CUSTOMER_ID")).isPassed(), "unique rule");

        List<RelationshipCandidate> relations = new RelationshipAnalyzer().analyze(customers, MockDatasets.orders());
        boolean sharedGender = false;
        for (RelationshipCandidate relation : relations) {
            if (relation.getKind() == RelationshipCandidate.Kind.SHARED_DICTIONARY
                    && relation.getLeft().endsWith("CUSTOMER.GENDER")
                    && relation.getRight().endsWith("ORDERS.GENDER")) sharedGender = true;
        }
        check(sharedGender, "shared dictionary relation");
        System.out.println("Manual regression checks passed: profile + 5 rules + "
                + relations.size() + " relationship candidate(s)");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError("failed: " + label);
    }
}
