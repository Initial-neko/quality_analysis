package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.persistence.TableProfileRecord;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord.ColumnRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;

import java.sql.Types;
import java.util.Locale;

/**
 * Central definition for one Excel field-detail column.
 * Header, width and rendering stay together so the delivery layout can be changed safely.
 */
enum ExcelDetailColumn {
    DATABASE("数据库", 16) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            text(row, index, table.database);
        }
    },
    SCHEMA("Schema", 16) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            text(row, index, table.schema);
        }
    },
    TABLE("表名", 22) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            text(row, index, table.table);
        }
    },
    COLUMN("字段名", 22) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            text(row, index, column.name);
        }
    },
    DB_TYPE("DB类型", 20) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            text(row, index, databaseType(column));
        }
    },
    PRIMARY_KEY("主键", 10) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            text(row, index, column.declaredPrimaryKey ? "是" : "");
        }
    },
    ROW_COUNT("总行数", 14) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            number(row, index, column.rowCount);
        }
    },
    NULL_COUNT("NULL数", 12) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            number(row, index, column.nullCount);
        }
    },
    NULL_RATE("NULL率", 12) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            percent(row, index, column.nullRate, styles.percent);
        }
    },
    MISSING_STRINGS("空串/语义空", 16) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            text(row, index, column.blankCount + " / " + column.semanticNullCount);
        }
    },
    DISTINCT_COUNT("Distinct数", 14) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            number(row, index, column.distinctCount);
        }
    },
    UNIQUENESS("唯一率", 12) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            percent(row, index, column.uniqueness, styles.percent);
        }
    },
    MIN_VALUE("最小值", 18) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            text(row, index, column.minValue);
        }
    },
    MAX_VALUE("最大值", 18) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            text(row, index, column.maxValue);
        }
    },
    LENGTH_SUMMARY("长度(最小/最大/平均)", 24) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            text(row, index, lengthSummary(column));
        }
    },
    VALUES("枚举/TopN", 42) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            wrapped(row, index, ExcelProfileReportWriter.valueSummary(column.values), styles.wrap);
        }
    },
    INSIGHTS("探查提示", 28) {
        @Override void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles) {
            wrapped(row, index, ExcelProfileReportWriter.insightSummary(column), styles.wrap);
        }
    };

    final String header;
    final int widthCharacters;

    ExcelDetailColumn(String header, int widthCharacters) {
        this.header = header;
        this.widthCharacters = widthCharacters;
    }

    abstract void write(Row row, int index, TableProfileRecord table, ColumnRecord column, Styles styles);

    static String databaseType(ColumnRecord column) {
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

    private static String lengthSummary(ColumnRecord column) {
        if (column.minLength == null && column.maxLength == null && column.avgLength == null) return "";
        return numberText(column.minLength) + " / " + numberText(column.maxLength) + " / "
                + (column.avgLength == null ? "" : String.format(Locale.ROOT, "%.2f", column.avgLength));
    }

    private static String numberText(Number value) { return value == null ? "" : String.valueOf(value); }

    private static void text(Row row, int index, String value) {
        row.createCell(index).setCellValue(value == null ? "" : value);
    }

    private static void number(Row row, int index, long value) {
        row.createCell(index).setCellValue((double) value);
    }

    private static void percent(Row row, int index, double value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void wrapped(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    static final class Styles {
        final CellStyle percent;
        final CellStyle wrap;

        Styles(CellStyle percent, CellStyle wrap) {
            this.percent = percent;
            this.wrap = wrap;
        }
    }
}
