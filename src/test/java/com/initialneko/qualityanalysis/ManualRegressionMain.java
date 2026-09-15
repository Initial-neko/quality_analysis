package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.RelationshipCandidate;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.relation.RelationshipAnalyzer;

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

        List<RelationshipCandidate> relations = new RelationshipAnalyzer().analyze(customers, MockDatasets.orders());
        boolean sharedGender = false;
        for (RelationshipCandidate relation : relations) {
            if (relation.getKind() == RelationshipCandidate.Kind.SHARED_DICTIONARY
                    && relation.getLeft().endsWith("CUSTOMER.GENDER")
                    && relation.getRight().endsWith("ORDERS.GENDER")) sharedGender = true;
        }
        check(sharedGender, "shared dictionary relation");
        System.out.println("Manual regression checks passed: profile + "
                + relations.size() + " optional relationship candidate(s)");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError("failed: " + label);
    }
}
