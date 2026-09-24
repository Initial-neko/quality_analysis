package com.initialneko.qualityanalysis.report.descriptor;

import com.initialneko.qualityanalysis.report.annotation.CellType;

/**
 * One resolved report column: display metadata plus a value extractor bound to
 * the concrete access path (field / method / flattened chain).
 */
public final class ColumnDescriptor {
    /** Extracts a column value from a row object; may return null. */
    public interface Accessor {
        Object get(Object row);
    }

    private final int order;
    private final String header;
    private final int width;
    private final CellType type;
    private final Accessor accessor;

    public ColumnDescriptor(int order, String header, int width, CellType type, Accessor accessor) {
        this.order = order;
        this.header = header;
        this.width = width;
        this.type = type;
        this.accessor = accessor;
    }

    public int getOrder() { return order; }
    public String getHeader() { return header; }
    public int getWidth() { return width; }
    public CellType getType() { return type; }
    public Accessor getAccessor() { return accessor; }

    public Object extract(Object row) { return accessor.get(row); }
}
