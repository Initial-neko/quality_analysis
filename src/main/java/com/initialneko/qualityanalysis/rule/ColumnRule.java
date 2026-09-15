package com.initialneko.qualityanalysis.rule;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.ColumnProfile;

/**
 * Extension point for configured streaming validation rules.
 *
 * One rule instance is stateful and should be bound to one column for one scan.
 * Rules may inspect every raw value in {@link #accept(Object)} and may also reuse
 * the completed {@link ColumnProfile} in {@link #finish(ColumnProfile)}.
 */
public interface ColumnRule {
    String getId();
    void start(ColumnMetadata metadata, ProfileOptions options);
    void accept(Object value);
    RuleResult finish(ColumnProfile profile);
}
