package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.jdbc.JdbcTableProfiler;
import org.junit.Test;

import static org.junit.Assert.*;

public class JdbcScanPlanTest {
    @Test
    public void scanSqlHasNoPaginationAndNoPrimaryKeyOrdering() {
        String sql = new JdbcTableProfiler().buildScanSql("APP", "CUSTOMER");
        assertEquals("SELECT * FROM APP.CUSTOMER", sql);
        assertFalse(sql.toUpperCase().contains("ORDER BY"));
        assertFalse(sql.toUpperCase().contains("OFFSET"));
        assertFalse(sql.toUpperCase().contains("LIMIT"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsafeIdentifiers() {
        new JdbcTableProfiler().buildScanSql("APP", "CUSTOMER;DROP TABLE X");
    }
}
