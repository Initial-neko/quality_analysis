package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.jdbc.LobValue;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.TableMetadata;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.ValueFamily;
import com.initialneko.qualityanalysis.profile.ProfileEngine;

import java.sql.Date;
import java.sql.Types;
import java.util.Arrays;

final class MockDatasets {
    private MockDatasets() {}

    static TableProfile customers() {
        TableMetadata metadata = customerMetadata();
        ProfileEngine engine = new ProfileEngine(metadata, richTestOptions());
        for (int i = 1; i <= 200; i++) {
            String status;
            if (i == 197) status = " active ";
            else if (i == 198) status = "Active";
            else if (i % 10 == 0) status = "PENDING";
            else if (i % 3 == 0) status = "INACTIVE";
            else status = "ACTIVE";
            String gender = i % 3 == 0 ? "U" : (i % 2 == 0 ? "F" : "M");
            int age = i == 1 ? -1 : (i == 2 ? 150 : 18 + (i % 60));
            String phone = i == 199 ? "138-1234-5678" : String.format("138%08d", i);
            Date date = i == 200 ? Date.valueOf("2099-12-31") : Date.valueOf("2026-09-15");
            Object optionalCode = i == 1 ? null : (i == 2 ? "" : (i == 3 ? "NULL" : "C" + (i % 4)));
            engine.acceptRow(Long.valueOf(i), status, gender, Integer.valueOf(age), phone, date, "CRM",
                    LobValue.clob(100L + i, null), LobValue.blob(2048L + i), optionalCode);
        }
        return engine.finish();
    }

    static TableProfile orders() {
        TableMetadata metadata = new TableMetadata("TEST", "ORDERS", Arrays.asList(
                col(1, "ORDER_ID", Types.BIGINT, "BIGINT", false, true, ValueFamily.NUMBER),
                col(2, "CUSTOMER_ID", Types.BIGINT, "BIGINT", false, false, ValueFamily.NUMBER),
                col(3, "GENDER", Types.VARCHAR, "VARCHAR", true, false, ValueFamily.STRING),
                col(4, "ORDER_STATUS", Types.VARCHAR, "VARCHAR", true, false, ValueFamily.STRING)
        ));
        ProfileEngine engine = new ProfileEngine(metadata, richTestOptions());
        for (int i = 1; i <= 120; i++) {
            String gender = i % 3 == 0 ? "U" : (i % 2 == 0 ? "F" : "M");
            String status = i % 5 == 0 ? "CANCELLED" : (i % 2 == 0 ? "DONE" : "WAITING");
            engine.acceptRow(Long.valueOf(10_000 + i), Long.valueOf(i), gender, status);
        }
        return engine.finish();
    }

    static TableMetadata customerMetadata() {
        return new TableMetadata("TEST", "CUSTOMER", Arrays.asList(
                col(1, "CUSTOMER_ID", Types.BIGINT, "BIGINT", false, true, ValueFamily.NUMBER),
                col(2, "STATUS", Types.VARCHAR, "VARCHAR", true, false, ValueFamily.STRING),
                col(3, "GENDER", Types.VARCHAR, "VARCHAR", true, false, ValueFamily.STRING),
                col(4, "AGE", Types.INTEGER, "INTEGER", true, false, ValueFamily.NUMBER),
                col(5, "PHONE", Types.VARCHAR, "VARCHAR", true, false, ValueFamily.STRING),
                col(6, "CREATE_DATE", Types.DATE, "DATE", true, false, ValueFamily.DATE_TIME),
                col(7, "SOURCE_SYSTEM", Types.VARCHAR, "VARCHAR", true, false, ValueFamily.STRING),
                col(8, "NOTE", Types.CLOB, "CLOB", true, false, ValueFamily.LOB),
                col(9, "PAYLOAD", Types.BLOB, "BLOB", true, false, ValueFamily.LOB),
                col(10, "OPTIONAL_CODE", Types.VARCHAR, "VARCHAR", true, false, ValueFamily.STRING)
        ));
    }

    static ProfileOptions richTestOptions() {
        return ProfileOptions.builder()
                .patternProfileEnabled(true)
                .caseVariantTrackingEnabled(true)
                .relationshipFingerprintEnabled(true)
                .build();
    }

    private static ColumnMetadata col(int ordinal, String name, int jdbcType, String dbType,
                                      boolean nullable, boolean pk, ValueFamily family) {
        return new ColumnMetadata(ordinal, name, name, jdbcType, dbType, 0, 0, nullable, pk, family);
    }
}
