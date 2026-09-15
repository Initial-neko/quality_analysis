package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.model.RelationshipCandidate;
import com.initialneko.qualityanalysis.relation.RelationshipAnalyzer;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class RelationshipAnalyzerTest {
    @Test
    public void detectsSharedDictionaryFromSetFingerprint() {
        List<RelationshipCandidate> candidates = new RelationshipAnalyzer().analyze(MockDatasets.customers(), MockDatasets.orders());
        boolean found = false;
        for (RelationshipCandidate candidate : candidates) {
            if (candidate.getKind() == RelationshipCandidate.Kind.SHARED_DICTIONARY
                    && candidate.getLeft().endsWith("CUSTOMER.GENDER")
                    && candidate.getRight().endsWith("ORDERS.GENDER")) {
                found = true;
                break;
            }
        }
        assertTrue("expected shared GENDER dictionary", found);
    }
}
