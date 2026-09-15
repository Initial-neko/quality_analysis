package com.initialneko.qualityanalysis.run;

import com.initialneko.qualityanalysis.QualityAnalyzer;
import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.TableRef;
import com.initialneko.qualityanalysis.persistence.ProfileRunStore;
import com.initialneko.qualityanalysis.persistence.RunManifest;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord;
import com.initialneko.qualityanalysis.report.ProfileReportGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Executes tables sequentially and durably persists each completed profile. */
public final class ProfileRunService {
    private final QualityAnalyzer analyzer;

    public ProfileRunService() {
        this(new QualityAnalyzer());
    }

    ProfileRunService(QualityAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Path run(Connection connection, String databaseLabel, List<TableRef> tables,
                    ProfileOptions options, Path outputRoot) throws IOException, SQLException {
        if (connection == null) throw new IllegalArgumentException("connection is null");
        if (tables == null || tables.isEmpty()) throw new IllegalArgumentException("tables is empty");
        if (options == null) options = ProfileOptions.defaults();
        if (outputRoot == null) throw new IllegalArgumentException("outputRoot is null");

        String runId = createRunId();
        Path runDirectory = outputRoot.resolve(runId);
        Files.createDirectories(runDirectory);
        ProfileRunStore store = new ProfileRunStore(runDirectory);
        RunManifest manifest = RunManifest.start(runId, databaseLabel, tables.size(), options);
        store.writeManifest(manifest);

        SQLException firstFailure = null;
        for (TableRef table : tables) {
            long started = System.currentTimeMillis();
            try {
                TableProfile profile = analyzer.profileTable(connection, table.getSchema(), table.getTable(), options);
                long finished = System.currentTimeMillis();
                TableProfileRecord record = TableProfileRecord.from(databaseLabel, profile, finished, finished - started);
                String recordFile = store.writeTableRecord(record);
                manifest.tableSucceeded(table.getSchema(), table.getTable(), recordFile,
                        profile.getRowCount(), finished - started);
            } catch (SQLException failure) {
                if (firstFailure == null) firstFailure = failure;
                manifest.tableFailed(table.getSchema(), table.getTable(), safeMessage(failure));
            }
            store.writeManifest(manifest);
        }

        manifest.finish();
        store.writeManifest(manifest);
        new ProfileReportGenerator().generateAll(runDirectory);

        if (firstFailure != null && manifest.successTables == 0) throw firstFailure;
        return runDirectory;
    }

    private static String createRunId() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ROOT).format(new Date());
    }

    private static String safeMessage(SQLException failure) {
        String message = failure.getMessage();
        if (message == null || message.trim().length() == 0) return failure.getClass().getSimpleName();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
