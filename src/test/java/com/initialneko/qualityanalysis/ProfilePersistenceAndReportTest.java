package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.persistence.ProfileRunStore;
import com.initialneko.qualityanalysis.persistence.RunManifest;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord;
import com.initialneko.qualityanalysis.report.HtmlProfileReportWriter;
import com.initialneko.qualityanalysis.report.ProfileReportGenerator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ProfilePersistenceAndReportTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void persistsEachTableAndReadsItBack() throws Exception {
        Path runDirectory = temporaryFolder.newFolder("run-store").toPath();
        ProfileRunStore store = new ProfileRunStore(runDirectory);
        RunManifest manifest = RunManifest.start("run-001", "DM_TEST", 2, ProfileOptions.defaults());
        store.writeManifest(manifest);

        TableProfileRecord customers = TableProfileRecord.from("DM_TEST", MockDatasets.customers(), 1000L, 120L);
        String recordFile = store.writeTableRecord(customers);
        manifest.tableSucceeded("TEST", "CUSTOMER", recordFile, customers.rowCount, customers.durationMillis);
        store.writeManifest(manifest);

        assertTrue(Files.exists(runDirectory.resolve(recordFile)));
        TableProfileRecord loaded = store.readTableRecord("TEST", "CUSTOMER");
        assertEquals("CUSTOMER", loaded.table);
        assertEquals(200L, loaded.rowCount);
        assertEquals(10, loaded.columns.size());
        assertEquals(5L, find(loaded, "STATUS").distinctCount);
        assertTrue(find(loaded, "CUSTOMER_ID").candidatePrimaryKey);

        RunManifest loadedManifest = store.readManifest();
        assertEquals(1, loadedManifest.successTables);
        assertEquals("RUNNING", loadedManifest.status);
    }

    @Test
    public void generatesExcelAndHtmlOnlyFromPersistedRecords() throws Exception {
        Path runDirectory = temporaryFolder.newFolder("run-report").toPath();
        ProfileRunStore store = new ProfileRunStore(runDirectory);
        RunManifest manifest = RunManifest.start("run-002", "DM_TEST", 2, ProfileOptions.defaults());

        TableProfileRecord customers = TableProfileRecord.from("DM_TEST", MockDatasets.customers(), 1000L, 120L);
        String customersFile = store.writeTableRecord(customers);
        manifest.tableSucceeded("TEST", "CUSTOMER", customersFile, customers.rowCount, customers.durationMillis);

        TableProfileRecord orders = TableProfileRecord.from("DM_TEST", MockDatasets.orders(), 1100L, 80L);
        String ordersFile = store.writeTableRecord(orders);
        manifest.tableSucceeded("TEST", "ORDERS", ordersFile, orders.rowCount, orders.durationMillis);
        manifest.finish();
        store.writeManifest(manifest);

        new ProfileReportGenerator().generateAll(runDirectory);
        Path excel = runDirectory.resolve(ProfileReportGenerator.EXCEL_FILE);
        Path html = runDirectory.resolve(ProfileReportGenerator.HTML_FILE);
        Path customerHtml = runDirectory.resolve(HtmlProfileReportWriter.TABLE_PAGE_DIRECTORY).resolve("TEST.CUSTOMER.html");
        Path ordersHtml = runDirectory.resolve(HtmlProfileReportWriter.TABLE_PAGE_DIRECTORY).resolve("TEST.ORDERS.html");
        assertTrue(Files.size(excel) > 0L);
        assertTrue(Files.size(html) > 0L);
        assertTrue(Files.size(customerHtml) > 0L);
        assertTrue(Files.size(ordersHtml) > 0L);

        InputStream in = Files.newInputStream(excel);
        Workbook workbook = new XSSFWorkbook(in);
        try {
            assertNotNull(workbook.getSheet("扫描概览"));
            Sheet detail = workbook.getSheet("字段质量明细");
            assertNotNull(detail);
            assertEquals("主明细应保持精简，避免再次膨胀", 17, detail.getRow(0).getLastCellNum());
            assertTrue(containsCell(detail, "CUSTOMER"));
            assertTrue(containsCell(detail, "PHONE"));
            assertTrue(containsCell(detail, "空串/语义空"));
            assertTrue(containsCell(detail, "探查提示"));
            assertFalse(containsCell(detail, "JDBC类型"));
            assertFalse(containsCell(detail, "类型族"));
            assertFalse(containsCell(detail, "LOB内容跳过"));
            assertNotNull(workbook.getSheet("探查提示"));
        } finally {
            workbook.close();
            in.close();
        }

        String htmlText = new String(Files.readAllBytes(html), StandardCharsets.UTF_8);
        assertTrue(htmlText.contains("数据质量 Profile 报告"));
        assertTrue(htmlText.contains("CUSTOMER"));
        assertTrue(htmlText.contains("quality-profile-tables/TEST.CUSTOMER.html"));
        assertFalse("首页不应再铺开字段明细", htmlText.contains("PHONE"));
        assertFalse("首页不应再包含字段明细表头", htmlText.contains("Distinct/唯一率"));

        String customerHtmlText = new String(Files.readAllBytes(customerHtml), StandardCharsets.UTF_8);
        assertTrue(customerHtmlText.contains("TEST.CUSTOMER"));
        assertTrue(customerHtmlText.contains("PHONE"));
        assertTrue(customerHtmlText.contains("潜在枚举"));
        assertTrue(customerHtmlText.contains("Distinct/唯一率"));
        assertTrue(customerHtmlText.contains("<div>Min："));
        assertTrue(customerHtmlText.contains("<div>Max："));
        assertTrue(customerHtmlText.contains("class=\"values\""));
        assertTrue(customerHtmlText.contains("../quality-profile.html"));
    }

    private static TableProfileRecord.ColumnRecord find(TableProfileRecord record, String column) {
        for (TableProfileRecord.ColumnRecord item : record.columns) {
            if (item.name.equalsIgnoreCase(column)) return item;
        }
        fail("missing column " + column);
        return null;
    }

    private static boolean containsCell(Sheet sheet, String expected) {
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            if (sheet.getRow(r) == null) continue;
            for (int c = 0; c < sheet.getRow(r).getLastCellNum(); c++) {
                if (sheet.getRow(r).getCell(c) != null && expected.equals(sheet.getRow(r).getCell(c).toString())) return true;
            }
        }
        return false;
    }
}
