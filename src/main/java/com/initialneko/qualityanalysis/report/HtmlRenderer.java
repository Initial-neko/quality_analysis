package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.report.descriptor.ColumnDescriptor;
import com.initialneko.qualityanalysis.report.descriptor.ReportDescriptor;

import java.util.List;
import java.util.Locale;

/**
 * Renders a list of rows as a self-contained HTML {@code <table>} driven by a
 * {@link ReportDescriptor}. Uses the same lightweight StringBuilder style as
 * the rest of the report (no template engine, no new dependencies).
 */
public final class HtmlRenderer {
    private HtmlRenderer() {}

    public static String renderTable(List<?> rows, ReportDescriptor descriptor) {
        StringBuilder html = new StringBuilder(8 * 1024);
        html.append("<table class=\"detail-table\"><thead><tr>");
        for (ColumnDescriptor column : descriptor.getColumns()) {
            html.append("<th>").append(escape(column.getHeader())).append("</th>");
        }
        html.append("</tr></thead><tbody>");
        if (rows != null) {
            for (Object row : rows) {
                html.append("<tr>");
                for (ColumnDescriptor column : descriptor.getColumns()) {
                    appendCell(html, column, column.extract(row));
                }
                html.append("</tr>");
            }
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private static void appendCell(StringBuilder html, ColumnDescriptor column, Object value) {
        if (value == null) {
            html.append("<td></td>");
            return;
        }
        switch (column.getType()) {
            case PERCENT:
                html.append("<td class=\"metric\">").append(value instanceof Number
                        ? percent(((Number) value).doubleValue()) : escape(String.valueOf(value))).append("</td>");
                return;
            case WRAP:
                html.append("<td class=\"values\">").append(escape(String.valueOf(value))).append("</td>");
                return;
            case NUMBER:
            case DATE:
            case TEXT:
            default:
                html.append("<td>").append(escape(String.valueOf(value))).append("</td>");
        }
    }

    static String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0d);
    }

    static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
