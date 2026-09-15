package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.persistence.RunManifest;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord.ColumnRecord;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord.ValueRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Writes a compact Excel delivery report from persisted records. */
public final class ExcelProfileReportWriter {
    private static final String[] DETAIL_HEADERS = {
            "数据库", "Schema", "表名", "字段名", "字段标签", "DB类型", "JDBC类型", "类型族",
            "可空", "声明主键", "总行数", "NULL数", "NULL率", "空串数", "语义空值数", "非空数",
            "Distinct数", "唯一率", "最小值", "最大值", "最小长度", "最大长度", "平均长度",
            "潜在枚举", "枚举/TopN", "候选唯一键", "常量字段", "准常量", "LOB内容跳过"
    };

    public void write(RunManifest manifest, List<TableProfileRecord> records, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        Workbook workbook = new XSSFWorkbook();
        try {
            Styles styles = new Styles(workbook);
            writeSummary(workbook, styles, manifest, records);
            writeFieldDetails(workbook, styles, records);
            writeInsights(workbook, styles, records);
            OutputStream out = Files.newOutputStream(output);
            try {
                workbook.write(out);
            } finally {
                out.close();
            }
        } finally {
            workbook.close();
        }
    }

    private void writeSummary(Workbook workbook, Styles styles, RunManifest manifest,
                              List<TableProfileRecord> records) {
        Sheet sheet = workbook.createSheet("扫描概览");
        Row title = sheet.createRow(0);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("数据质量 Profile 报告");
        titleCell.setCellStyle(styles.title);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        long fields = 0L;
        long rows = 0L;
        long enums = 0L;
        long keys = 0L;
        long constants = 0L;
        for (TableProfileRecord record : records) {
            rows += record.rowCount;
            fields += record.columns.size();
            for (ColumnRecord column : record.columns) {
                if (column.potentialEnum) enums++;
                if (column.candidatePrimaryKey) keys++;
                if (column.constant) constants++;
            }
        }

        String[][] values = {
                {"运行ID", safe(manifest.runId)},
                {"数据库", safe(manifest.database)},
                {"运行状态", safe(manifest.status)},
                {"开始时间", formatTime(manifest.startedAtEpochMillis)},
                {"结束时间", formatTime(manifest.finishedAtEpochMillis)},
                {"计划表数", String.valueOf(manifest.totalTables)},
                {"成功表数", String.valueOf(manifest.successTables)},
                {"失败表数", String.valueOf(manifest.failedTables)},
                {"已保存表记录", String.valueOf(records.size())},
                {"字段总数", String.valueOf(fields)},
                {"累计扫描行数", String.valueOf(rows)},
                {"潜在枚举字段", String.valueOf(enums)},
                {"候选唯一键字段", String.valueOf(keys)},
                {"常量字段", String.valueOf(constants)},
                {"fetchSize", manifest.options == null ? "" : String.valueOf(manifest.options.fetchSize)}
        };
        for (int i = 0; i < values.length; i++) {
            Row row = sheet.createRow(i + 2);
            Cell key = row.createCell(0);
            key.setCellValue(values[i][0]);
            key.setCellStyle(styles.label);
            Cell value = row.createCell(1);
            value.setCellValue(values[i][1]);
        }
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 32 * 256);
    }

    private void writeFieldDetails(Workbook workbook, Styles styles, List<TableProfileRecord> records) {
        Sheet sheet = workbook.createSheet("字段质量明细");
        writeHeader(sheet, styles, DETAIL_HEADERS);
        int rowIndex = 1;
        for (TableProfileRecord record : records) {
            for (ColumnRecord column : record.columns) {
                Row row = sheet.createRow(rowIndex++);
                int c = 0;
                set(row, c++, record.database);
                set(row, c++, record.schema);
                set(row, c++, record.table);
                set(row, c++, column.name);
                set(row, c++, column.label);
                set(row, c++, column.databaseTypeName);
                set(row, c++, column.jdbcType);
                set(row, c++, column.family);
                set(row, c++, yesNo(column.nullable));
                set(row, c++, yesNo(column.declaredPrimaryKey));
                set(row, c++, column.rowCount);
                set(row, c++, column.nullCount);
                setPercent(row, c++, column.nullRate, styles.percent);
                set(row, c++, column.blankCount);
                set(row, c++, column.semanticNullCount);
                set(row, c++, column.nonNullCount);
                set(row, c++, column.distinctCount);
                setPercent(row, c++, column.uniqueness, styles.percent);
                set(row, c++, column.minValue);
                set(row, c++, column.maxValue);
                setNullableNumber(row, c++, column.minLength);
                setNullableNumber(row, c++, column.maxLength);
                setNullableNumber(row, c++, column.avgLength);
                set(row, c++, yesNo(column.potentialEnum));
                set(row, c++, valueSummary(column.values));
                set(row, c++, yesNo(column.candidatePrimaryKey));
                set(row, c++, yesNo(column.constant));
                set(row, c++, yesNo(column.quasiConstant));
                set(row, c, yesNo(column.lobContentSkipped));
            }
        }
        sheet.createFreezePane(0, 1);
        if (rowIndex > 1) sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, DETAIL_HEADERS.length - 1));
        int[] widths = {16,16,22,22,22,16,12,12,10,12,14,12,12,12,14,14,14,12,18,18,12,12,12,12,42,14,12,12,14};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
    }

    private void writeInsights(Workbook workbook, Styles styles, List<TableProfileRecord> records) {
        Sheet sheet = workbook.createSheet("探查提示");
        String[] headers = {"数据库", "Schema", "表名", "字段名", "提示类型", "说明"};
        writeHeader(sheet, styles, headers);
        int rowIndex = 1;
        for (TableProfileRecord record : records) {
            for (ColumnRecord column : record.columns) {
                if (column.potentialEnum) {
                    rowIndex = insight(sheet, rowIndex, record, column, "潜在枚举", valueSummary(column.values));
                }
                if (column.candidatePrimaryKey) {
                    rowIndex = insight(sheet, rowIndex, record, column, "候选唯一键",
                            "非空且唯一率=" + percentText(column.uniqueness));
                }
                if (column.constant) {
                    rowIndex = insight(sheet, rowIndex, record, column, "常量字段",
                            "Distinct=1" + (column.values.isEmpty() ? "" : ", value=" + column.values.get(0).value));
                } else if (column.quasiConstant) {
                    rowIndex = insight(sheet, rowIndex, record, column, "准常量字段", "值高度集中");
                }
            }
        }
        sheet.createFreezePane(0, 1);
        if (rowIndex > 1) sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, headers.length - 1));
        int[] widths = {16,16,22,22,18,64};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
    }

    private static int insight(Sheet sheet, int rowIndex, TableProfileRecord record,
                               ColumnRecord column, String type, String detail) {
        Row row = sheet.createRow(rowIndex++);
        set(row, 0, record.database);
        set(row, 1, record.schema);
        set(row, 2, record.table);
        set(row, 3, column.name);
        set(row, 4, type);
        set(row, 5, detail);
        return rowIndex;
    }

    private static void writeHeader(Sheet sheet, Styles styles, String[] headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header);
        }
    }

    private static void set(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private static void set(Row row, int column, long value) {
        row.createCell(column).setCellValue((double) value);
    }

    private static void set(Row row, int column, int value) {
        row.createCell(column).setCellValue((double) value);
    }

    private static void setPercent(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void setNullableNumber(Row row, int column, Number value) {
        if (value == null) set(row, column, "");
        else row.createCell(column).setCellValue(value.doubleValue());
    }

    private static String valueSummary(List<ValueRecord> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (ValueRecord value : values) {
            if (out.length() > 0) out.append("; ");
            out.append(value.value == null ? "<NULL>" : value.value)
                    .append("(").append(value.count).append(", ").append(percentText(value.ratio)).append(")");
            if (out.length() > 30000) {
                out.append(" ...");
                break;
            }
        }
        return out.toString();
    }

    private static String yesNo(boolean value) { return value ? "是" : "否"; }
    private static String safe(String value) { return value == null ? "" : value; }

    private static String percentText(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0d);
    }

    private static String formatTime(long epochMillis) {
        if (epochMillis <= 0L) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date(epochMillis));
    }

    private static final class Styles {
        private final CellStyle title;
        private final CellStyle header;
        private final CellStyle label;
        private final CellStyle percent;

        private Styles(Workbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            title = workbook.createCellStyle();
            title.setFont(titleFont);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            header = workbook.createCellStyle();
            header.setFont(headerFont);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setFillForegroundColor((short) 22);
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            label = workbook.createCellStyle();
            label.setFont(labelFont);

            percent = workbook.createCellStyle();
            percent.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        }
    }
}
