package com.initialneko.qualityanalysis.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TableMetadata {
    private final String schema;
    private final String table;
    private final List<ColumnMetadata> columns;

    public TableMetadata(String schema, String table, List<ColumnMetadata> columns) {
        this.schema = schema;
        this.table = table;
        this.columns = Collections.unmodifiableList(new ArrayList<ColumnMetadata>(columns));
    }

    public String getSchema() { return schema; }
    public String getTable() { return table; }
    public List<ColumnMetadata> getColumns() { return columns; }
}
