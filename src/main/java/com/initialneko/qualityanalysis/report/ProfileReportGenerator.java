package com.initialneko.qualityanalysis.report;

import com.initialneko.qualityanalysis.persistence.ProfileRunStore;
import com.initialneko.qualityanalysis.persistence.RunManifest;
import com.initialneko.qualityanalysis.persistence.TableProfileRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Generates all human-facing reports from persisted profile records only. */
public final class ProfileReportGenerator {
    public static final String EXCEL_FILE = "quality-profile.xlsx";
    public static final String HTML_FILE = "quality-profile.html";

    public void generateAll(Path runDirectory) throws IOException {
        ProfileRunStore store = new ProfileRunStore(runDirectory);
        RunManifest manifest = store.readManifest();
        List<TableProfileRecord> records = store.readAllTableRecords();
        new ExcelProfileReportWriter().write(manifest, records, runDirectory.resolve(EXCEL_FILE));
        new HtmlProfileReportWriter().write(manifest, records, runDirectory.resolve(HTML_FILE));
    }
}
