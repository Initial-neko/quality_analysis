package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.persistence.TableProfileRecord.ColumnRecord;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;

public class DatabaseTypeFormatterTest {
    @Test
    public void formatsCharacterLengthAndNumericPrecisionScale() {
        ColumnRecord varchar = column(Types.VARCHAR, "VARCHAR", 100, 0);
        assertEquals("VARCHAR(100)", DatabaseTypeFormatter.format(varchar));

        ColumnRecord decimal = column(Types.DECIMAL, "DECIMAL", 18, 2);
        assertEquals("DECIMAL(18,2)", DatabaseTypeFormatter.format(decimal));

        ColumnRecord number = column(Types.NUMERIC, "NUMBER", 20, 0);
        assertEquals("NUMBER(20,0)", DatabaseTypeFormatter.format(number));
    }

    @Test
    public void doesNotInventLengthForIntegerOrUnknownPrecision() {
        ColumnRecord integer = column(Types.INTEGER, "INTEGER", 10, 0);
        assertEquals("INTEGER", DatabaseTypeFormatter.format(integer));

        ColumnRecord varcharUnknown = column(Types.VARCHAR, "VARCHAR", 0, 0);
        assertEquals("VARCHAR", DatabaseTypeFormatter.format(varcharUnknown));
    }

    private static ColumnRecord column(int jdbcType, String typeName, int precision, int scale) {
        ColumnRecord column = new ColumnRecord();
        column.jdbcType = jdbcType;
        column.databaseTypeName = typeName;
        column.precision = precision;
        column.scale = scale;
        return column;
    }
}
