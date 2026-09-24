package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.report.descriptor.ColumnDescriptor;
import com.initialneko.qualityanalysis.report.descriptor.ReportDescriptor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;

/**
 * Renders a list of rows as an Excel sheet driven by a {@link ReportDescriptor}:
 * header row, column widths, freeze pane, auto filter and per-cell styles all
 * come from the shared column annotations.
 */
public final class ExcelRenderer {
    private ExcelRenderer() {}

    /** The cell styles the renderer needs; built once per workbook by the caller. */
    public static final class Styles {
        public final CellStyle header;
        public final CellStyle percent;
        public final CellStyle wrap;

        public Styles(CellStyle header, CellStyle percent, CellStyle wrap) {
            this.header = header;
            this.percent = percent;
            this.wrap = wrap;
        }
    }

    public static void renderSheet(Workbook workbook, String sheetName, List<?> rows,
                                   ReportDescriptor descriptor, Styles styles) {
        Sheet sheet = workbook.createSheet(sheetName);
        List<ColumnDescriptor> columns = descriptor.getColumns();

        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns.get(i).getHeader());
            cell.setCellStyle(styles.header);
        }

        int rowIndex = 1;
        if (rows != null) {
            for (Object row : rows) {
                Row sheetRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < columns.size(); i++) {
                    appendCell(sheetRow, i, columns.get(i), columns.get(i).extract(row), styles);
                }
            }
        }

        sheet.createFreezePane(0, 1);
        if (rowIndex > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, columns.size() - 1));
        }
        for (int i = 0; i < columns.size(); i++) {
            sheet.setColumnWidth(i, columns.get(i).getWidth() * 256);
        }
    }

    private static void appendCell(Row row, int index, ColumnDescriptor column, Object value, Styles styles) {
        Cell cell = row.createCell(index);
        if (value == null) {
            cell.setCellValue("");
            return;
        }
        switch (column.getType()) {
            case PERCENT:
                cell.setCellValue(value instanceof Number ? ((Number) value).doubleValue() : 0.0d);
                cell.setCellStyle(styles.percent);
                return;
            case NUMBER:
                cell.setCellValue(value instanceof Number ? ((Number) value).doubleValue() : 0.0d);
                return;
            case WRAP:
                cell.setCellValue(String.valueOf(value));
                cell.setCellStyle(styles.wrap);
                return;
            case DATE:
            case TEXT:
            default:
                cell.setCellValue(String.valueOf(value));
        }
    }
}
