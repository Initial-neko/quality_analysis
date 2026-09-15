package com.initialneko.qualityanalysis.model;

public final class TableRef {
    private final String schema;
    private final String table;

    public TableRef(String schema, String table) {
        this.schema = schema;
        this.table = table;
    }

    public String getSchema() { return schema; }
    public String getTable() { return table; }
}
