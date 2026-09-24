package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.persistence.TableProfileRecord;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord.ColumnRecord;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord.ValueRecord;
import com.initialneko.qualityanalysis.report.annotation.CellType;
import com.initialneko.qualityanalysis.report.annotation.DerivedColumn;
import com.initialneko.qualityanalysis.report.annotation.Flatten;
import com.initialneko.qualityanalysis.report.annotation.ReportTable;

import java.util.List;
import java.util.Locale;

/**
 * The reportable entity behind the field-quality detail table. Annotating this
 * class is enough to render both the HTML detail table and the Excel detail
 * sheet; adding a column means adding one {@link DerivedColumn} method here.
 */
@ReportTable(title = "字段质量明细", sheet = "字段质量明细")
public final class DetailRow {
    /** Table context columns join the table via flattening (kept for future use). */
    @Flatten
    public final TableProfileRecord table;
    /** Column statistics; all rendered columns below derive from it. */
    @Flatten
    public final ColumnRecord column;

    public DetailRow(TableProfileRecord table, ColumnRecord column) {
        this.table = table;
        this.column = column;
    }

    @DerivedColumn(order = 10, header = "数据库", width = 16)
    public String database() { return table.database; }

    @DerivedColumn(order = 11, header = "Schema", width = 16)
    public String schema() { return table.schema; }

    @DerivedColumn(order = 12, header = "表名", width = 22)
    public String table() { return table.table; }

    @DerivedColumn(order = 20, header = "字段名", width = 22)
    public String name() { return column.name; }

    @DerivedColumn(order = 21, header = "DB类型", width = 20)
    public String dbType() { return DatabaseTypeFormatter.format(column); }

    @DerivedColumn(order = 22, header = "主键", width = 10)
    public String primaryKey() { return column.declaredPrimaryKey ? "是" : ""; }

    @DerivedColumn(order = 23, header = "总行数", width = 14, type = CellType.NUMBER)
    public Long rowCount() { return column.rowCount; }

    @DerivedColumn(order = 24, header = "NULL数", width = 12, type = CellType.NUMBER)
    public Long nullCount() { return column.nullCount; }

    @DerivedColumn(order = 25, header = "NULL率", width = 12, type = CellType.PERCENT)
    public Double nullRate() { return column.nullRate; }

    @DerivedColumn(order = 26, header = "空串/语义空", width = 16)
    public String missingStrings() { return column.blankCount + " / " + column.semanticNullCount; }

    @DerivedColumn(order = 27, header = "Distinct数", width = 14, type = CellType.NUMBER)
    public Long distinctCount() { return column.distinctCount; }

    @DerivedColumn(order = 28, header = "唯一率", width = 12, type = CellType.PERCENT)
    public Double uniqueness() { return column.uniqueness; }

    @DerivedColumn(order = 29, header = "最小值", width = 18)
    public String minValue() { return column.minValue; }

    @DerivedColumn(order = 30, header = "最大值", width = 18)
    public String maxValue() { return column.maxValue; }

    @DerivedColumn(order = 31, header = "长度(最小/最大/平均)", width = 24)
    public String lengthSummary() {
        if (column.minLength == null && column.maxLength == null && column.avgLength == null) return "";
        return numberText(column.minLength) + " / " + numberText(column.maxLength) + " / "
                + (column.avgLength == null ? "" : String.format(Locale.ROOT, "%.2f", column.avgLength));
    }

    @DerivedColumn(order = 32, header = "枚举/TopN", width = 42, type = CellType.WRAP)
    public String valuesSummary() { return ExcelProfileReportWriter.valueSummary(column.values); }

    @DerivedColumn(order = 33, header = "探查提示", width = 28, type = CellType.WRAP)
    public String insights() { return ExcelProfileReportWriter.insightSummary(column); }

    @DerivedColumn(order = 40, header = "预设类型", width = 14)
    public String presetTypeName() {
        return column.presetValidation == null ? "" : nullSafe(column.presetValidation.presetTypeName);
    }

    @DerivedColumn(order = 41, header = "预设匹配率", width = 12, type = CellType.PERCENT)
    public Double presetMatchRate() {
        return column.presetValidation == null ? null : column.presetValidation.matchRate;
    }

    @DerivedColumn(order = 42, header = "预设匹配数", width = 12, type = CellType.NUMBER)
    public Long presetMatchedCount() {
        return column.presetValidation == null ? null : column.presetValidation.matchedCount;
    }

    @DerivedColumn(order = 43, header = "预设不匹配数", width = 12, type = CellType.NUMBER)
    public Long presetUnmatchedCount() {
        return column.presetValidation == null ? null : column.presetValidation.unmatchedCount;
    }

    @DerivedColumn(order = 44, header = "不匹配样本", width = 36, type = CellType.WRAP)
    public String presetUnmatchedSamples() {
        if (column.presetValidation == null) return "";
        StringBuilder out = new StringBuilder();
        for (String sample : column.presetValidation.unmatchedSamples) {
            if (out.length() > 0) out.append("; ");
            out.append(sample);
        }
        return out.toString();
    }

    private static String nullSafe(String value) { return value == null ? "" : value; }

    private static String numberText(Number value) { return value == null ? "" : String.valueOf(value); }

}
