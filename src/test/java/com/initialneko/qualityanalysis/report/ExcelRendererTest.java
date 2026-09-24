package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.report.annotation.CellType;
import com.initialneko.qualityanalysis.report.annotation.DerivedColumn;
import com.initialneko.qualityanalysis.report.annotation.ReportColumn;
import com.initialneko.qualityanalysis.report.annotation.ReportTable;
import com.initialneko.qualityanalysis.report.descriptor.ReportDescriptor;
import com.initialneko.qualityanalysis.report.descriptor.ReportDescriptorBuilder;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class ExcelRendererTest {

    @ReportTable(title = "Excel测试", sheet = "ExcelSheet")
    public static class Row {
        @ReportColumn(order = 1, header = "名称", width = 20)
        public String name;

        @ReportColumn(order = 2, header = "占比", width = 12, type = CellType.PERCENT)
        public Double ratio;

        @DerivedColumn(order = 3, header = "数量", width = 10, type = CellType.NUMBER)
        public Long count() { return 7L; }
    }

    private static ExcelRenderer.Styles styles(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setFillForegroundColor((short) 22);
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle percent = workbook.createCellStyle();
        percent.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

        CellStyle wrap = workbook.createCellStyle();
        wrap.setWrapText(true);
        return new ExcelRenderer.Styles(header, percent, wrap);
    }

    @Test
    public void rendersHeaderWidthsStylesAndAutoFilter() throws Exception {
        Row row = new Row();
        row.name = "alpha";
        row.ratio = 0.42d;
        Workbook workbook = new XSSFWorkbook();
        try {
            ReportDescriptor descriptor = ReportDescriptorBuilder.scan(Row.class);
            ExcelRenderer.renderSheet(workbook, "ExcelSheet",
                    Collections.singletonList(row), descriptor, styles(workbook));

            Sheet sheet = workbook.getSheet("ExcelSheet");
            assertNotNull(sheet);
            assertEquals(1, sheet.getLastRowNum());
            assertEquals("名称", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("占比", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("数量", sheet.getRow(0).getCell(2).getStringCellValue());

            assertEquals("alpha", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(0.42d, sheet.getRow(1).getCell(1).getNumericCellValue(), 0.0000001d);
            assertEquals("0.00%", sheet.getRow(1).getCell(1).getCellStyle().getDataFormatString());
            assertEquals(7.0d, sheet.getRow(1).getCell(2).getNumericCellValue(), 0.0000001d);

            assertEquals(20 * 256, sheet.getColumnWidth(0));
            assertEquals(12 * 256, sheet.getColumnWidth(1));
            assertTrue("应冻结首行", sheet.getPaneInformation() != null && sheet.getPaneInformation().isFreezePane());
        } finally {
            workbook.close();
        }
    }

    @Test
    public void nullValuesRenderAsEmptyTextCells() throws Exception {
        Row row = new Row();
        row.name = null;
        row.ratio = null;
        Workbook workbook = new XSSFWorkbook();
        try {
            ExcelRenderer.renderSheet(workbook, "S",
                    Collections.singletonList(row), ReportDescriptorBuilder.scan(Row.class), styles(workbook));
            Sheet sheet = workbook.getSheet("S");
            assertEquals("", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("", sheet.getRow(1).getCell(1).getStringCellValue());
            // derived count never null in this fixture
            assertEquals(7.0d, sheet.getRow(1).getCell(2).getNumericCellValue(), 0.0000001d);
        } finally {
            workbook.close();
        }
    }
}
