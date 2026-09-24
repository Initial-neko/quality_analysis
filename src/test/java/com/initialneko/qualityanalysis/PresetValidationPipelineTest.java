package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.PresetValidation;
import com.initialneko.qualityanalysis.model.TableMetadata;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.ValueFamily;
import com.initialneko.qualityanalysis.profile.ProfileEngine;
import org.junit.Test;

import java.sql.Types;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * End-to-end pipeline: sample the first N rows, classify string columns against
 * the preset regex registry, then validate the whole scan in the same pass.
 */
public class PresetValidationPipelineTest {

    private static TableProfile profile(int rows, ProfileOptions options) {
        return profile(rows, false, options);
    }

    private static TableProfile profile(int rows, boolean injectInvalid, ProfileOptions options) {
        ColumnMetadata phone = new ColumnMetadata(1, "PHONE", "PHONE", Types.VARCHAR,
                "VARCHAR", 20, 0, true, false, ValueFamily.STRING);
        ColumnMetadata email = new ColumnMetadata(2, "EMAIL", "EMAIL", Types.VARCHAR,
                "VARCHAR", 64, 0, true, false, ValueFamily.STRING);
        ColumnMetadata name = new ColumnMetadata(3, "NAME", "NAME", Types.VARCHAR,
                "VARCHAR", 32, 0, true, false, ValueFamily.STRING);
        TableMetadata metadata = new TableMetadata("TEST", "T",
                java.util.Arrays.asList(phone, email, name));
        ProfileEngine engine = new ProfileEngine(metadata, options);
        for (int i = 0; i < rows; i++) {
            String phoneValue = injectInvalid && i == rows - 1 ? "notaphone" : "1381234" + String.format("%04d", i);
            engine.acceptRow(phoneValue, "user" + i + "@example.com", "张三" + i);
        }
        return engine.finish();
    }

    @Test
    public void classifiesFromSampleAndValidatesWholeScan() {
        TableProfile result = profile(12, true, ProfileOptions.defaults());

        PresetValidation phone = result.findColumn("PHONE").getPresetValidation();
        assertNotNull("手机号列应被识别为预设类型", phone);
        assertEquals("手机号", phone.getPresetTypeName());
        assertEquals(11L, phone.getMatchedCount());
        assertEquals(1L, phone.getUnmatchedCount());
        assertEquals(11.0d / 12.0d, phone.getMatchRate(), 0.000001d);
        assertEquals(1, phone.getUnmatchedSamples().size());
        assertEquals("notaphone", phone.getUnmatchedSamples().get(0));

        PresetValidation email = result.findColumn("EMAIL").getPresetValidation();
        assertNotNull(email);
        assertEquals("邮箱", email.getPresetTypeName());
        assertEquals(12L, email.getMatchedCount());
        assertEquals(0L, email.getUnmatchedCount());
        assertEquals(1.0d, email.getMatchRate(), 0.000001d);

        assertNull("姓名列不应被误判", result.findColumn("NAME").getPresetValidation());
    }

    @Test
    public void smallTableUsesAllRowsAsSample() {
        TableProfile result = profile(5, false, ProfileOptions.defaults());
        PresetValidation phone = result.findColumn("PHONE").getPresetValidation();
        assertNotNull("行数少于样本数时应用全部行识别", phone);
        assertEquals("手机号", phone.getPresetTypeName());
        assertEquals(5L, phone.getMatchedCount());
        assertEquals(0L, phone.getUnmatchedCount());
    }

    @Test
    public void disabledOptionProducesNoValidation() {
        TableProfile result = profile(12, true,
                ProfileOptions.builder().presetValidationEnabled(false).build());
        assertNull(result.findColumn("PHONE").getPresetValidation());
        assertNull(result.findColumn("EMAIL").getPresetValidation());
    }
}
