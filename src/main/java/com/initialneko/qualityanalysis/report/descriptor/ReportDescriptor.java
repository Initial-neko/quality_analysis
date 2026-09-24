package com.initialneko.qualityanalysis.report.descriptor;

import java.util.Collections;
import java.util.List;

/**
 * The resolved column set of one reportable entity. This is the single source
 * of truth shared by the HTML and Excel renderers.
 */
public final class ReportDescriptor {
    private final Class<?> type;
    private final String title;
    private final String sheetName;
    private final List<ColumnDescriptor> columns;

    public ReportDescriptor(Class<?> type, String title, String sheetName, List<ColumnDescriptor> columns) {
        this.type = type;
        this.title = title;
        this.sheetName = sheetName;
        this.columns = Collections.unmodifiableList(columns);
    }

    public Class<?> getType() { return type; }
    public String getTitle() { return title; }
    public String getSheetName() { return sheetName; }
    public List<ColumnDescriptor> getColumns() { return columns; }
}
