package com.initialneko.qualityanalysis.profile;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.ColumnProfile;
import com.initialneko.qualityanalysis.model.TableMetadata;
import com.initialneko.qualityanalysis.model.TableProfile;

import java.util.ArrayList;
import java.util.List;

public final class ProfileEngine {
    private final TableMetadata metadata;
    private final ColumnAccumulator[] accumulators;
    private long rowCount;
    private boolean rowOpen;
    private int acceptedCells;

    public ProfileEngine(TableMetadata metadata, ProfileOptions options) {
        this.metadata = metadata;
        List<ColumnMetadata> columns = metadata.getColumns();
        this.accumulators = new ColumnAccumulator[columns.size()];
        for (int i = 0; i < columns.size(); i++) accumulators[i] = new ColumnAccumulator(columns.get(i), options);
    }

    public void beginRow() {
        if (rowOpen) throw new IllegalStateException("previous row is still open");
        rowOpen = true;
        acceptedCells = 0;
    }

    public void acceptCell(int zeroBasedColumnIndex, Object value) {
        if (!rowOpen) throw new IllegalStateException("beginRow() must be called first");
        if (zeroBasedColumnIndex < 0 || zeroBasedColumnIndex >= accumulators.length) throw new IndexOutOfBoundsException("column index: " + zeroBasedColumnIndex);
        accumulators[zeroBasedColumnIndex].accept(value);
        acceptedCells++;
    }

    public void endRow() {
        if (!rowOpen) throw new IllegalStateException("no row is open");
        if (acceptedCells != accumulators.length) throw new IllegalStateException("expected " + accumulators.length + " cells but accepted " + acceptedCells);
        rowCount++;
        rowOpen = false;
    }

    public void acceptRow(Object... values) {
        if (values.length != accumulators.length) throw new IllegalArgumentException("expected " + accumulators.length + " values but got " + values.length);
        beginRow();
        for (int i = 0; i < values.length; i++) acceptCell(i, values[i]);
        endRow();
    }

    public TableProfile finish() {
        if (rowOpen) throw new IllegalStateException("cannot finish while a row is open");
        List<ColumnProfile> profiles = new ArrayList<ColumnProfile>(accumulators.length);
        for (ColumnAccumulator accumulator : accumulators) profiles.add(accumulator.finish());
        return new TableProfile(metadata, rowCount, profiles);
    }
}
