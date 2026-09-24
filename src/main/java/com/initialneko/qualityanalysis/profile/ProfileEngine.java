package com.initialneko.qualityanalysis.profile;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.TableMetadata;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.regex.PresetRegexRule;
import com.initialneko.qualityanalysis.regex.PresetSampleClassifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-pass profiling engine. When preset validation is enabled, the first
 * N rows are buffered, each string column is classified against the preset
 * regex registry, and the winning rule is attached to the column accumulator
 * so the remaining rows are validated in the same scan (one table, one query).
 */
public final class ProfileEngine {
    private final TableMetadata metadata;
    private final ColumnAccumulator[] accumulators;
    private final ProfileOptions options;
    private final boolean presetEnabled;
    private final int presetSampleSize;
    private final List<Object[]> presetSampleRows = new ArrayList<Object[]>();
    private Object[] presetCurrentRow;
    private boolean presetRulesApplied;
    private long rowCount;
    private boolean rowOpen;
    private int acceptedCells;

    public ProfileEngine(TableMetadata metadata, ProfileOptions options) {
        this.metadata = metadata;
        this.options = options;
        List<ColumnMetadata> columns = metadata.getColumns();
        this.accumulators = new ColumnAccumulator[columns.size()];
        for (int i = 0; i < columns.size(); i++) accumulators[i] = new ColumnAccumulator(columns.get(i), options);
        this.presetEnabled = options.isPresetValidationEnabled();
        this.presetSampleSize = options.getPresetSampleSize();
    }

    public void beginRow() {
        if (rowOpen) throw new IllegalStateException("previous row is still open");
        rowOpen = true;
        acceptedCells = 0;
        if (presetEnabled && !presetRulesApplied) presetCurrentRow = new Object[accumulators.length];
    }

    public void acceptCell(int zeroBasedColumnIndex, Object value) {
        if (!rowOpen) throw new IllegalStateException("beginRow() must be called first");
        if (zeroBasedColumnIndex < 0 || zeroBasedColumnIndex >= accumulators.length) throw new IndexOutOfBoundsException("column index: " + zeroBasedColumnIndex);
        accumulators[zeroBasedColumnIndex].accept(value);
        if (presetCurrentRow != null) presetCurrentRow[zeroBasedColumnIndex] = value;
        acceptedCells++;
    }

    public void endRow() {
        if (!rowOpen) throw new IllegalStateException("no row is open");
        if (acceptedCells != accumulators.length) throw new IllegalStateException("expected " + accumulators.length + " cells but accepted " + acceptedCells);
        rowCount++;
        rowOpen = false;
        if (presetCurrentRow != null) {
            presetSampleRows.add(presetCurrentRow);
            presetCurrentRow = null;
            if (rowCount >= presetSampleSize) applyPresetRules();
        }
    }

    public void acceptRow(Object... values) {
        if (values.length != accumulators.length) throw new IllegalArgumentException("expected " + accumulators.length + " values but got " + values.length);
        beginRow();
        for (int i = 0; i < values.length; i++) acceptCell(i, values[i]);
        endRow();
    }

    public TableProfile finish() {
        if (rowOpen) throw new IllegalStateException("cannot finish while a row is open");
        if (!presetRulesApplied && !presetSampleRows.isEmpty()) applyPresetRules();
        List<ColumnProfile> profiles = new ArrayList<ColumnProfile>(accumulators.length);
        for (ColumnAccumulator accumulator : accumulators) profiles.add(accumulator.finish());
        return new TableProfile(metadata, rowCount, profiles);
    }

    /** Classifies buffered sample rows per column, then backfills validation counts. */
    private void applyPresetRules() {
        presetRulesApplied = true;
        double threshold = options.getPresetMatchThresholdPct() / 100.0d;
        for (int i = 0; i < accumulators.length; i++) {
            List<String> sampled = sampledStrings(i);
            PresetSampleClassifier.Candidate candidate =
                    PresetSampleClassifier.classify(sampled, options.getPresetRegexRegistry(), threshold);
            if (candidate == null) continue;
            accumulators[i].setPresetRule(candidate.rule);
            for (Object[] row : presetSampleRows) {
                accumulators[i].recordPresetValue(row[i]);
            }
        }
        presetSampleRows.clear();
    }

    private List<String> sampledStrings(int columnIndex) {
        List<String> sampled = new ArrayList<String>(presetSampleRows.size());
        for (Object[] row : presetSampleRows) {
            Object value = row[columnIndex];
            if (value == null) continue;
            if (!(value instanceof String)) continue;
            String raw = (String) value;
            String normalized = options.isTrimStrings() ? raw.trim() : raw;
            if (normalized.length() == 0 || options.isSemanticNull(normalized)) continue;
            sampled.add(normalized);
        }
        return sampled;
    }
}
