package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.input.TableListFileReader;
import com.initialneko.qualityanalysis.model.TableRef;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TableListFileReaderTest {
    @Test
    public void readsDefaultSchemaQualifiedNamesCommentsAndDuplicates() throws Exception {
        Path file = Files.createTempFile("quality-analysis-tables", ".txt");
        try {
            Files.write(file, Arrays.asList(
                    "# only these tables are scanned",
                    "CUSTOMER",
                    " TEST.ORDERS ",
                    "",
                    "customer",
                    "OTHER.AUDIT_LOG"
            ), StandardCharsets.UTF_8);

            List<TableRef> tables = TableListFileReader.read(file, "TEST");
            assertEquals(3, tables.size());
            assertEquals("TEST", tables.get(0).getSchema());
            assertEquals("CUSTOMER", tables.get(0).getTable());
            assertEquals("TEST", tables.get(1).getSchema());
            assertEquals("ORDERS", tables.get(1).getTable());
            assertEquals("OTHER", tables.get(2).getSchema());
            assertEquals("AUDIT_LOG", tables.get(2).getTable());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void requiresSchemaWhenDefaultSchemaIsDisabled() throws Exception {
        Path file = Files.createTempFile("quality-analysis-tables", ".txt");
        try {
            Files.write(file, Arrays.asList("CUSTOMER"), StandardCharsets.UTF_8);
            TableListFileReader.read(file, "-");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void acceptsFullyQualifiedTablesWithoutDefaultSchema() throws Exception {
        Path file = Files.createTempFile("quality-analysis-tables", ".txt");
        try {
            Files.write(file, Arrays.asList("TEST.CUSTOMER", "SALES.ORDERS"), StandardCharsets.UTF_8);
            List<TableRef> tables = TableListFileReader.read(file, "-");
            assertEquals(2, tables.size());
            assertEquals("TEST", tables.get(0).getSchema());
            assertEquals("CUSTOMER", tables.get(0).getTable());
            assertEquals("SALES", tables.get(1).getSchema());
            assertEquals("ORDERS", tables.get(1).getTable());
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
