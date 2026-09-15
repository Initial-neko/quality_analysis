package com.initialneko.qualityanalysis.model;

public final class RelationshipCandidate {
    public enum Kind {
        SHARED_DICTIONARY,
        SAME_VALUE_DOMAIN,
        POTENTIAL_RELATION
    }

    private final Kind kind;
    private final String left;
    private final String right;
    private final double similarity;
    private final double nameSimilarity;
    private final String reason;

    public RelationshipCandidate(Kind kind, String left, String right, double similarity,
                                 double nameSimilarity, String reason) {
        this.kind = kind;
        this.left = left;
        this.right = right;
        this.similarity = similarity;
        this.nameSimilarity = nameSimilarity;
        this.reason = reason;
    }

    public Kind getKind() { return kind; }
    public String getLeft() { return left; }
    public String getRight() { return right; }
    public double getSimilarity() { return similarity; }
    public double getNameSimilarity() { return nameSimilarity; }
    public String getReason() { return reason; }
}
