package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.TableMetadata;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.ValueFamily;
import com.initialneko.qualityanalysis.profile.ProfileEngine;
import org.junit.Test;

import java.sql.Types;
import java.util.Collections;

import static org.junit.Assert.*;

public class MinimalProfileOptionsTest {
    @Test
    public void defaultsKeepCoreSignalsAndSkipOptionalEnrichment() {
        ProfileOptions options = ProfileOptions.defaults();
        assertTrue(options.isDistinctEnabled());
        assertTrue(options.isValueFrequencyEnabled());
        assertTrue(options.isRangeEnabled());
        assertTrue(options.isLengthEnabled());
        assertFalse(options.isPatternProfileEnabled());
        assertFalse(options.isStringShapeEnabled());
        assertFalse(options.isCaseVariantTrackingEnabled());
        assertFalse(options.isRelationshipFingerprintEnabled());
        assertEquals(0, options.getClobPreviewChars());

        ColumnMetadata column = new ColumnMetadata(1, "STATUS", "STATUS", Types.VARCHAR,
                "VARCHAR", 20, 0, true, false, ValueFamily.STRING);
        TableMetadata metadata = new TableMetadata("TEST", "T", Collections.singletonList(column));
        ProfileEngine engine = new ProfileEngine(metadata, options);
        engine.acceptRow("A");
        engine.acceptRow("B");
        engine.acceptRow("A");
        TableProfile profile = engine.finish();
        ColumnProfile status = profile.findColumn("STATUS");

        assertEquals(2L, status.getDistinctCount());
        assertEquals(2, status.getValues().size());
        assertTrue(status.getTopPatterns().isEmpty());
        assertEquals(0, status.getCaseVariantGroupCount());
        assertEquals(0, status.getSetFingerprint().getMinHashBins().length);
    }
}
