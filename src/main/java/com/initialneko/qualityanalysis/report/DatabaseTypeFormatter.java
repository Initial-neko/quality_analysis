package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.persistence.TableProfileRecord.ColumnRecord;

import java.sql.Types;

/** Formats persisted JDBC type metadata for human-facing reports. */
final class DatabaseTypeFormatter {
    private DatabaseTypeFormatter() { }

    static String format(ColumnRecord column) {
        String base = column.databaseTypeName == null ? "" : column.databaseTypeName;
        if (base.length() == 0) return "";
        int precision = column.precision;
        int scale = column.scale;
        switch (column.jdbcType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return precision > 0 ? base + "(" + precision + ")" : base;
            case Types.DECIMAL:
            case Types.NUMERIC:
                return precision > 0 ? base + "(" + precision + "," + Math.max(scale, 0) + ")" : base;
            default:
                return base;
        }
    }
}
