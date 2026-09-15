package com.initialneko.qualityanalysis.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TableProfile {
    private final TableMetadata metadata;
    private final long rowCount;
    private final List<ColumnProfile> columns;

    public TableProfile(TableMetadata metadata, long rowCount, List<ColumnProfile> columns) {
        this.metadata = metadata;
        this.rowCount = rowCount;
        this.columns = Collections.unmodifiableList(new ArrayList<ColumnProfile>(columns));
    }

    public TableMetadata getMetadata() { return metadata; }
    public long getRowCount() { return rowCount; }
    public List<ColumnProfile> getColumns() { return columns; }

    public ColumnProfile findColumn(String name) {
        for (ColumnProfile c : columns) {
            if (c.getMetadata().getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }
}
