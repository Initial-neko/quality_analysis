package com.initialneko.qualityanalysis.model;

public final class ColumnMetadata {
    private final int ordinal;
    private final String name;
    private final String label;
    private final int jdbcType;
    private final String databaseTypeName;
    private final int precision;
    private final int scale;
    private final boolean nullable;
    private final boolean declaredPrimaryKey;
    private final ValueFamily family;

    public ColumnMetadata(int ordinal, String name, String label, int jdbcType, String databaseTypeName,
                          int precision, int scale, boolean nullable, boolean declaredPrimaryKey,
                          ValueFamily family) {
        this.ordinal = ordinal;
        this.name = name;
        this.label = label;
        this.jdbcType = jdbcType;
        this.databaseTypeName = databaseTypeName;
        this.precision = precision;
        this.scale = scale;
        this.nullable = nullable;
        this.declaredPrimaryKey = declaredPrimaryKey;
        this.family = family;
    }

    public int getOrdinal() { return ordinal; }
    public String getName() { return name; }
    public String getLabel() { return label; }
    public int getJdbcType() { return jdbcType; }
    public String getDatabaseTypeName() { return databaseTypeName; }
    public int getPrecision() { return precision; }
    public int getScale() { return scale; }
    public boolean isNullable() { return nullable; }
    public boolean isDeclaredPrimaryKey() { return declaredPrimaryKey; }
    public ValueFamily getFamily() { return family; }
}
