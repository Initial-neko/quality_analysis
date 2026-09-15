package com.initialneko.qualityanalysis.jdbc;

public final class LobValue {
    public enum Kind { CLOB, BLOB }

    private final Kind kind;
    private final long length;
    private final String preview;

    private LobValue(Kind kind, long length, String preview) {
        this.kind = kind;
        this.length = length;
        this.preview = preview;
    }

    public static LobValue clob(long length, String preview) { return new LobValue(Kind.CLOB, length, preview); }
    public static LobValue blob(long length) { return new LobValue(Kind.BLOB, length, null); }
    public Kind getKind() { return kind; }
    public long getLength() { return length; }
    public String getPreview() { return preview; }
}
