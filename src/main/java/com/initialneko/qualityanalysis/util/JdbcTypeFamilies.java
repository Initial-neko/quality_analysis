package com.initialneko.qualityanalysis.util;

import com.initialneko.qualityanalysis.model.ValueFamily;

import java.sql.Types;

public final class JdbcTypeFamilies {
    private JdbcTypeFamilies() {}

    public static ValueFamily familyOf(int jdbcType) {
        switch (jdbcType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
                return ValueFamily.STRING;
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return ValueFamily.NUMBER;
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case 2013:
            case 2014:
                return ValueFamily.DATE_TIME;
            case Types.BOOLEAN:
            case Types.BIT:
                return ValueFamily.BOOLEAN;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                return ValueFamily.BINARY;
            case Types.CLOB:
            case Types.NCLOB:
            case Types.BLOB:
                return ValueFamily.LOB;
            default:
                return ValueFamily.OTHER;
        }
    }
}
