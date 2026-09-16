package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.persistence.RunManifest;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord.ColumnRecord;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord.ValueRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Generates a lightweight index plus one static HTML page per table. */
public final class HtmlProfileReportWriter {
    public static final String TABLE_PAGE_DIRECTORY = "quality-profile-tables";

    public void write(RunManifest manifest, List<TableProfileRecord> records, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        Path tableDirectory = output.getParent().resolve(TABLE_PAGE_DIRECTORY);
        prepareTableDirectory(tableDirectory);
        for (TableProfileRecord record : records) {
            writeTablePage(record, tableDirectory.resolve(tablePageFileName(record)));
        }
        writeIndex(manifest, records, output);
    }

    private void writeIndex(RunManifest manifest, List<TableProfileRecord> records, Path output) throws IOException {
        StringBuilder html = new StringBuilder(32 * 1024);
        appendHead(html, "数据质量 Profile 报告", false);
        html.append("<header><h1>数据质量 Profile 报告</h1><div class=\"header-muted\">运行ID：")
                .append(escape(manifest.runId)).append(" ｜ 数据库：").append(escape(manifest.database))
                .append(" ｜ ").append(escape(manifest.status)).append("</div></header><main>");

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

        html.append("<div class=\"cards\">");
        card(html, "已保存表", records.size());
        card(html, "字段总数", fields);
        card(html, "累计扫描行数", rows);
        card(html, "潜在枚举", enums);
        card(html, "候选唯一键", keys);
        card(html, "常量字段", constants);
        html.append("</div>");

        html.append("<div class=\"panel\"><b>运行信息</b><div class=\"muted metric\" style=\"margin-top:8px\">")
                .append("<div>开始：").append(escape(formatTime(manifest.startedAtEpochMillis))).append("</div>")
                .append("<div>结束：").append(escape(formatTime(manifest.finishedAtEpochMillis))).append("</div>")
                .append("<div>成功表：").append(manifest.successTables).append(" ｜ 失败表：").append(manifest.failedTables).append("</div>")
                .append("<div>fetchSize：").append(manifest.options == null ? "" : manifest.options.fetchSize).append("</div>")
                .append("</div></div>");

        html.append("<div class=\"panel\"><div class=\"panel-title-row\"><b>表概览</b><input id=\"tableSearch\" placeholder=\"搜索表名或Schema\" oninput=\"filterTableRows()\"></div>")
                .append("<table class=\"summary-table\"><thead><tr><th>数据库</th><th>Schema</th><th>表名</th><th>行数</th><th>字段数</th><th>潜在枚举</th><th>候选唯一键</th><th>常量字段</th><th>耗时(ms)</th></tr></thead><tbody id=\"tableRows\">");
        for (TableProfileRecord record : records) {
            int enumCount = 0;
            int keyCount = 0;
            int constantCount = 0;
            for (ColumnRecord column : record.columns) {
                if (column.potentialEnum) enumCount++;
                if (column.candidatePrimaryKey) keyCount++;
                if (column.constant) constantCount++;
            }
            String searchName = displayName(record).toLowerCase(Locale.ROOT);
            html.append("<tr data-name=\"").append(attr(searchName)).append("\"><td>").append(escape(record.database))
                    .append("</td><td>").append(escape(record.schema)).append("</td><td><a href=\"")
                    .append(TABLE_PAGE_DIRECTORY).append("/").append(attr(tablePageFileName(record))).append("\">")
                    .append(escape(record.table)).append("</a></td><td>").append(record.rowCount)
                    .append("</td><td>").append(record.columns.size()).append("</td><td>").append(enumCount)
                    .append("</td><td>").append(keyCount).append("</td><td>").append(constantCount)
                    .append("</td><td>").append(record.durationMillis).append("</td></tr>");
        }
        html.append("</tbody></table></div>");
        html.append("</main><script>function filterTableRows(){var q=document.getElementById('tableSearch').value.toLowerCase();document.querySelectorAll('#tableRows tr').forEach(function(e){e.style.display=e.dataset.name.indexOf(q)>=0?'':'none'});}</script></body></html>");
        Files.write(output, html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void writeTablePage(TableProfileRecord record, Path output) throws IOException {
        StringBuilder html = new StringBuilder(24 * 1024);
        String title = displayName(record) + " - Profile";
        appendHead(html, title, true);
        html.append("<header><div><a class=\"back-link\" href=\"../quality-profile.html\">← 返回表概览</a></div><h1>")
                .append(escape(displayName(record))).append("</h1><div class=\"header-muted\">数据库：")
                .append(escape(record.database)).append(" ｜ 扫描耗时：").append(record.durationMillis).append(" ms</div></header><main>");

        html.append("<div class=\"cards\">");
        card(html, "行数", record.rowCount);
        card(html, "字段数", record.columns.size());
        int enumCount = 0;
        int keyCount = 0;
        int constantCount = 0;
        for (ColumnRecord column : record.columns) {
            if (column.potentialEnum) enumCount++;
            if (column.candidatePrimaryKey) keyCount++;
            if (column.constant) constantCount++;
        }
        card(html, "潜在枚举", enumCount);
        card(html, "候选唯一键", keyCount);
        card(html, "常量字段", constantCount);
        html.append("</div>");

        html.append("<div class=\"panel\"><table class=\"detail-table\"><thead><tr><th>字段</th><th>类型</th><th>主键</th><th>NULL</th><th>空串/语义空</th><th>Distinct/唯一率</th><th>值域</th><th>长度</th><th>探查提示</th><th>枚举/TopN</th></tr></thead><tbody>");
        for (ColumnRecord column : record.columns) {
            html.append("<tr><td><b>").append(escape(column.name)).append("</b></td><td>")
                    .append(escape(column.databaseTypeName)).append("</td><td>").append(column.declaredPrimaryKey ? "是" : "")
                    .append("</td><td class=\"metric\"><div>数量：").append(column.nullCount).append("</div><div>比例：")
                    .append(percent(column.nullRate)).append("</div></td><td class=\"metric\"><div>空串：")
                    .append(column.blankCount).append("</div><div>语义空：").append(column.semanticNullCount)
                    .append("</div></td><td class=\"metric\"><div>Distinct：").append(column.distinctCount)
                    .append("</div><div>唯一率：").append(percent(column.uniqueness)).append("</div></td><td class=\"metric\"><div>Min：")
                    .append(escape(column.minValue)).append("</div><div>Max：").append(escape(column.maxValue))
                    .append("</div></td><td>").append(lengthHtml(column)).append("</td><td>").append(insightTags(column))
                    .append("</td><td class=\"values\">").append(valueSummaryHtml(column.values)).append("</td></tr>");
        }
        html.append("</tbody></table></div></main></body></html>");
        Files.write(output, html.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void appendHead(StringBuilder html, String title, boolean detailPage) {
        html.append("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>").append(escape(title)).append("</title><style>")
                .append("body{font-family:Arial,'Microsoft YaHei',sans-serif;margin:0;background:#f5f7fa;color:#1f2937}")
                .append("header{background:#111827;color:white;padding:24px 32px}header h1{margin:8px 0;font-size:26px}.header-muted{color:#d1d5db}")
                .append("main{padding:24px 32px;max-width:1600px;margin:auto}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px;margin:18px 0}")
                .append(".card{background:white;border:1px solid #e5e7eb;border-radius:10px;padding:14px}.card b{display:block;font-size:24px;margin-top:6px}")
                .append(".panel{background:white;border:1px solid #e5e7eb;border-radius:10px;margin:18px 0;padding:16px;overflow:auto}")
                .append(".panel-title-row{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:12px}")
                .append("table{border-collapse:collapse;width:100%;font-size:13px}th,td{border-bottom:1px solid #e5e7eb;padding:8px 10px;text-align:left;vertical-align:top}")
                .append("th{background:#f9fafb;position:sticky;top:0;white-space:nowrap}.detail-table td{white-space:normal;line-height:1.55}")
                .append(".summary-table td{white-space:nowrap}.metric div{margin:1px 0}.values{min-width:180px;max-width:360px;white-space:normal;overflow-wrap:anywhere}.values div{margin:1px 0}")
                .append(".tag{display:inline-block;border-radius:999px;background:#eef2ff;padding:2px 8px;margin:2px;font-size:12px}.muted{color:#6b7280}")
                .append("a{color:#2563eb;text-decoration:none}a:hover{text-decoration:underline}.back-link{color:#bfdbfe}")
                .append("input{padding:9px 12px;border:1px solid #d1d5db;border-radius:8px;width:min(420px,100%);box-sizing:border-box}")
                .append("@media(max-width:900px){main{padding:14px}.panel{padding:12px}.detail-table{min-width:980px}.panel-title-row{align-items:stretch;flex-direction:column}}")
                .append("</style></head><body>");
    }

    private static void prepareTableDirectory(Path tableDirectory) throws IOException {
        Files.createDirectories(tableDirectory);
        DirectoryStream<Path> stream = Files.newDirectoryStream(tableDirectory, "*.html");
        try {
            for (Path path : stream) Files.deleteIfExists(path);
        } finally {
            stream.close();
        }
    }

    private static void card(StringBuilder html, String label, long value) {
        html.append("<div class=\"card\"><span class=\"muted\">").append(escape(label)).append("</span><b>")
                .append(value).append("</b></div>");
    }

    private static String displayName(TableProfileRecord record) {
        String schema = record.schema == null || record.schema.length() == 0 ? "" : record.schema + ".";
        return schema + (record.table == null ? "" : record.table);
    }

    private static String tablePageFileName(TableProfileRecord record) {
        String schema = record.schema == null || record.schema.trim().length() == 0 ? "DEFAULT" : record.schema;
        return safeFilePart(schema) + "." + safeFilePart(record.table) + ".html";
    }

    private static String safeFilePart(String value) {
        if (value == null || value.length() == 0) return "UNKNOWN";
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.') out.append(c);
            else out.append('_');
        }
        return out.toString();
    }

    private static String insightTags(ColumnRecord column) {
        StringBuilder out = new StringBuilder();
        if (column.potentialEnum) tag(out, "潜在枚举");
        if (column.candidatePrimaryKey) tag(out, "候选唯一键");
        if (column.constant) tag(out, "常量字段");
        else if (column.quasiConstant) tag(out, "准常量");
        if (column.lobContentSkipped) tag(out, "LOB仅画像长度");
        return out.toString();
    }

    private static void tag(StringBuilder out, String text) {
        out.append("<span class=\"tag\">").append(escape(text)).append("</span>");
    }

    private static String valueSummaryHtml(List<ValueRecord> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (ValueRecord value : values) {
            out.append("<div>").append(escape(value.value == null ? "<NULL>" : value.value))
                    .append(" <span class=\"muted\">(").append(value.count).append(", ")
                    .append(percent(value.ratio)).append(")</span></div>");
        }
        return out.toString();
    }

    private static String lengthHtml(ColumnRecord column) {
        if (column.minLength == null && column.maxLength == null && column.avgLength == null) return "";
        StringBuilder out = new StringBuilder("<div class=\"metric\">");
        out.append("<div>最小：").append(escape(safeNumber(column.minLength))).append("</div>")
                .append("<div>最大：").append(escape(safeNumber(column.maxLength))).append("</div>")
                .append("<div>平均：").append(column.avgLength == null ? "" : String.format(Locale.ROOT, "%.2f", column.avgLength)).append("</div></div>");
        return out.toString();
    }

    private static String safeNumber(Number value) { return value == null ? "" : String.valueOf(value); }
    private static String percent(double value) { return String.format(Locale.ROOT, "%.2f%%", value * 100.0d); }

    private static String formatTime(long epochMillis) {
        if (epochMillis <= 0L) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date(epochMillis));
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String attr(String value) { return escape(value); }
}
