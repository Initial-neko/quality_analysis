package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.report.annotation.CellType;
import com.initialneko.qualityanalysis.report.annotation.DerivedColumn;
import com.initialneko.qualityanalysis.report.annotation.ReportColumn;
import com.initialneko.qualityanalysis.report.annotation.ReportTable;
import com.initialneko.qualityanalysis.report.descriptor.ReportDescriptor;
import com.initialneko.qualityanalysis.report.descriptor.ReportDescriptorBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class HtmlRendererTest {

    @ReportTable(title = "HTML测试", sheet = "HTML测试")
    public static class Row {
        @ReportColumn(order = 1, header = "名称")
        public String name;

        @ReportColumn(order = 2, header = "占比", type = CellType.PERCENT)
        public Double ratio;

        @DerivedColumn(order = 3, header = "备注", type = CellType.WRAP)
        public String note() { return "note-" + name; }
    }

    private static ReportDescriptor descriptor() { return ReportDescriptorBuilder.scan(Row.class); }

    @Test
    public void rendersHeadersRowsPercentAndWrap() {
        Row row = new Row();
        row.name = "alpha";
        row.ratio = 0.1234d;
        List<Row> rows = Collections.singletonList(row);
        String html = HtmlRenderer.renderTable(rows, descriptor());

        assertTrue(html.contains("<table class=\"detail-table\">"));
        assertTrue(html.contains("<th>名称</th>"));
        assertTrue(html.contains("<th>占比</th>"));
        assertTrue(html.contains("<th>备注</th>"));
        assertTrue(html.contains("<td>alpha</td>"));
        assertTrue(html.contains("12.34%"));
        assertTrue(html.contains("class=\"values\""));
        assertTrue(html.contains("note-alpha"));
    }

    @Test
    public void escapesTextAndHandlesNullValues() {
        Row row = new Row();
        row.name = "<b>&\"'</b>";
        row.ratio = null;
        String html = HtmlRenderer.renderTable(Collections.singletonList(row), descriptor());

        assertTrue(html.contains("&lt;b&gt;&amp;&quot;&#39;&lt;/b&gt;"));
        assertTrue(html.contains("<td></td>"));
    }

    @Test
    public void emptyRowsProduceHeaderOnlyTable() {
        String html = HtmlRenderer.renderTable(Arrays.<Row>asList(), descriptor());
        assertTrue(html.contains("<th>名称</th>"));
        assertFalse(html.contains("<td>"));
    }
}
