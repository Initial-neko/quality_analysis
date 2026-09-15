package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.TableProfile;
import org.junit.Test;

import static org.junit.Assert.*;

public class MockProfileRegressionTest {
    @Test
    public void profilesCoreMetricsAndLowCardinalityEnums() {
        TableProfile profile = MockDatasets.customers();
        assertEquals(200L, profile.getRowCount());
        ColumnProfile id = profile.findColumn("CUSTOMER_ID");
        assertTrue(id.isCandidatePrimaryKey());
        assertTrue(id.getMetadata().isDeclaredPrimaryKey());
        assertEquals(200L, id.getDistinctCount());
        assertEquals(1.0d, id.getUniqueness(), 0.000001d);
        ColumnProfile status = profile.findColumn("STATUS");
        assertTrue(status.isLowCardinality());
        assertEquals(5L, status.getDistinctCount());
        assertEquals(5, status.getValues().size());
        assertEquals(1L, status.getTrimChangedCount());
        assertTrue(status.getCaseVariantGroupCount() >= 1);
        ColumnProfile source = profile.findColumn("SOURCE_SYSTEM");
        assertTrue(source.isConstant());
        assertTrue(source.isQuasiConstant());
        assertEquals(1L, source.getDistinctCount());
    }

    @Test
    public void profilesNullSemanticNullRangePatternAndLobWithoutMaterializingContent() {
        TableProfile profile = MockDatasets.customers();
        ColumnProfile optional = profile.findColumn("OPTIONAL_CODE");
        assertEquals(1L, optional.getNullCount());
        assertEquals(1L, optional.getBlankCount());
        assertEquals(1L, optional.getSemanticNullCount());
        ColumnProfile age = profile.findColumn("AGE");
        assertEquals("-1", age.getMinValue());
        assertEquals("150", age.getMaxValue());
        ColumnProfile phone = profile.findColumn("PHONE");
        assertFalse(phone.getTopPatterns().isEmpty());
        assertEquals("D11", phone.getTopPatterns().get(0).getPattern());
        ColumnProfile note = profile.findColumn("NOTE");
        assertTrue(note.isLobContentSkipped());
        assertEquals(0L, note.getDistinctCount());
        assertEquals(Long.valueOf(101L), note.getMinLength());
        assertEquals(Long.valueOf(300L), note.getMaxLength());
        ColumnProfile payload = profile.findColumn("PAYLOAD");
        assertTrue(payload.isLobContentSkipped());
        assertEquals(Long.valueOf(2049L), payload.getMinLength());
        assertEquals(Long.valueOf(2248L), payload.getMaxLength());
    }
}
