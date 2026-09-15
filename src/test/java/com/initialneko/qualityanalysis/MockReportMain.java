package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.persistence.ProfileRunStore;
import com.initialneko.qualityanalysis.persistence.RunManifest;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord;
import com.initialneko.qualityanalysis.report.ProfileReportGenerator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Generates a visible Profile V1 report from deterministic mock data without any database. */
public final class MockReportMain {
    private MockReportMain() {}

    public static void main(String[] args) throws Exception {
        Path output = Paths.get(args.length > 0 ? args[0] : "target/mock-profile-report").toAbsolutePath();
        deleteRecursively(output.toFile());
        Files.createDirectories(output);

        ProfileOptions options = ProfileOptions.defaults();
        ProfileRunStore store = new ProfileRunStore(output);
        RunManifest manifest = RunManifest.start("mock-profile-v1", "MOCK_DB", 2, options);
        store.writeManifest(manifest);

        persist(store, manifest, "CUSTOMER", MockDatasets.customers(options), 120L);
        persist(store, manifest, "ORDERS", MockDatasets.orders(options), 80L);

        manifest.finish();
        store.writeManifest(manifest);
        new ProfileReportGenerator().generateAll(output);

        System.out.println("Mock Profile 报告已生成：" + output);
        System.out.println("- manifest: " + output.resolve("manifest.json"));
        System.out.println("- CUSTOMER JSON: " + output.resolve("tables/TEST.CUSTOMER.json"));
        System.out.println("- ORDERS JSON: " + output.resolve("tables/TEST.ORDERS.json"));
        System.out.println("- Excel: " + output.resolve(ProfileReportGenerator.EXCEL_FILE));
        System.out.println("- HTML : " + output.resolve(ProfileReportGenerator.HTML_FILE));
        System.out.println("Mock 数据：CUSTOMER=200行/10字段，ORDERS=120行/4字段。Profile 使用当前 V1 默认配置。 ");
    }

    private static void persist(ProfileRunStore store, RunManifest manifest, String tableName,
                                TableProfile profile, long durationMillis) throws Exception {
        long scannedAt = System.currentTimeMillis();
        TableProfileRecord record = TableProfileRecord.from("MOCK_DB", profile, scannedAt, durationMillis);
        String file = store.writeTableRecord(record);
        manifest.tableSucceeded("TEST", tableName, file, record.rowCount, durationMillis);
        store.writeManifest(manifest);
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        if (!file.delete()) throw new IllegalStateException("无法删除旧 Mock 输出: " + file);
    }
}
