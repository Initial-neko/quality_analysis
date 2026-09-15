package com.initialneko.qualityanalysis.relation;

import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.RelationshipCandidate;
import com.initialneko.qualityanalysis.model.SetFingerprint;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.ValueFamily;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RelationshipAnalyzer {
    private final double potentialSimilarityThreshold;

    public RelationshipAnalyzer() { this(0.45d); }
    public RelationshipAnalyzer(double potentialSimilarityThreshold) { this.potentialSimilarityThreshold = potentialSimilarityThreshold; }

    public List<RelationshipCandidate> analyze(TableProfile left, TableProfile right) {
        List<RelationshipCandidate> result = new ArrayList<RelationshipCandidate>();
        for (ColumnProfile a : left.getColumns()) {
            if (!eligible(a)) continue;
            for (ColumnProfile b : right.getColumns()) {
                if (!eligible(b) || !compatible(a, b)) continue;
                SetFingerprint fa = a.getSetFingerprint();
                SetFingerprint fb = b.getSetFingerprint();
                if (fa.getDistinctCount() == 0 || fb.getDistinctCount() == 0) continue;
                String leftRef = ref(left, a);
                String rightRef = ref(right, b);
                double nameSim = nameSimilarity(a.getMetadata().getName(), b.getMetadata().getName());
                if (fa.exactSetFingerprintEquals(fb)) {
                    RelationshipCandidate.Kind kind = fa.getDistinctCount() <= 100
                            ? RelationshipCandidate.Kind.SHARED_DICTIONARY
                            : RelationshipCandidate.Kind.SAME_VALUE_DOMAIN;
                    result.add(new RelationshipCandidate(kind, leftRef, rightRef, 1.0d, nameSim,
                            "distinct count and three independent 64-bit set fingerprint aggregates match"));
                    continue;
                }
                double similarity = minHashSimilarity(fa.getMinHashBins(), fb.getMinHashBins());
                if (similarity >= potentialSimilarityThreshold && nameSim >= 0.20d) {
                    result.add(new RelationshipCandidate(RelationshipCandidate.Kind.POTENTIAL_RELATION,
                            leftRef, rightRef, similarity, nameSim,
                            "compatible type, similar column name and approximate distinct-set overlap"));
                }
            }
        }
        Collections.sort(result, new Comparator<RelationshipCandidate>() {
            @Override public int compare(RelationshipCandidate a, RelationshipCandidate b) {
                int kind = a.getKind().compareTo(b.getKind());
                if (kind != 0) return kind;
                return Double.compare(b.getSimilarity(), a.getSimilarity());
            }
        });
        return result;
    }

    private static boolean eligible(ColumnProfile p) {
        ValueFamily f = p.getMetadata().getFamily();
        return !p.isLobContentSkipped() && f != ValueFamily.LOB && f != ValueFamily.BINARY;
    }

    private static boolean compatible(ColumnProfile a, ColumnProfile b) {
        ValueFamily fa = a.getMetadata().getFamily();
        ValueFamily fb = b.getMetadata().getFamily();
        return fa == fb || fa == ValueFamily.OTHER || fb == ValueFamily.OTHER;
    }

    private static String ref(TableProfile table, ColumnProfile column) {
        String schema = table.getMetadata().getSchema();
        String prefix = schema == null || schema.length() == 0 ? "" : schema + ".";
        return prefix + table.getMetadata().getTable() + "." + column.getMetadata().getName();
    }

    static double minHashSimilarity(long[] a, long[] b) {
        int length = Math.min(a.length, b.length);
        int comparable = 0;
        int match = 0;
        for (int i = 0; i < length; i++) {
            boolean emptyA = a[i] == Long.MAX_VALUE;
            boolean emptyB = b[i] == Long.MAX_VALUE;
            if (emptyA && emptyB) continue;
            comparable++;
            if (!emptyA && !emptyB && a[i] == b[i]) match++;
        }
        return comparable == 0 ? 0.0d : (double) match / (double) comparable;
    }

    static double nameSimilarity(String left, String right) {
        String a = normalizeName(left);
        String b = normalizeName(right);
        if (a.equals(b)) return 1.0d;
        Set<String> ta = tokens(a);
        Set<String> tb = tokens(b);
        Set<String> intersection = new HashSet<String>(ta);
        intersection.retainAll(tb);
        Set<String> union = new HashSet<String>(ta);
        union.addAll(tb);
        double tokenScore = union.isEmpty() ? 0.0d : (double) intersection.size() / (double) union.size();
        if (a.endsWith(b) || b.endsWith(a)) tokenScore = Math.max(tokenScore, 0.6d);
        return tokenScore;
    }

    private static String normalizeName(String name) {
        if (name == null) return "";
        String s = name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9]+", "_");
        return s.replaceAll("^_+|_+$", "");
    }

    private static Set<String> tokens(String name) {
        Set<String> out = new HashSet<String>();
        for (String t : name.split("_+")) {
            if (t.length() == 0) continue;
            if ("id".equals(t) || "code".equals(t) || "no".equals(t) || "key".equals(t)) continue;
            out.add(t);
        }
        if (out.isEmpty() && name.length() > 0) out.add(name);
        return out;
    }
}
