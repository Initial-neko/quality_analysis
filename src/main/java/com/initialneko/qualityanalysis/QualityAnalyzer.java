package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.jdbc.JdbcTableProfiler;
import com.initialneko.qualityanalysis.model.AnalysisResult;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.TableRef;
import com.initialneko.qualityanalysis.rule.RuleBinding;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class QualityAnalyzer {
    private final JdbcTableProfiler jdbcTableProfiler = new JdbcTableProfiler();

    public TableProfile profileTable(Connection connection, String schema, String table) throws SQLException {
        return profileTable(connection, schema, table, ProfileOptions.defaults());
    }

    public TableProfile profileTable(Connection connection, String schema, String table, ProfileOptions options) throws SQLException {
        return jdbcTableProfiler.profile(connection, schema, table, options);
    }

    public AnalysisResult analyzeTable(Connection connection, String schema, String table,
                                       ProfileOptions options, List<RuleBinding> rules) throws SQLException {
        return jdbcTableProfiler.analyze(connection, schema, table, options, rules);
    }

    public List<TableProfile> profileTables(Connection connection, List<TableRef> tables, ProfileOptions options) throws SQLException {
        List<TableProfile> result = new ArrayList<TableProfile>(tables.size());
        for (TableRef table : tables) result.add(profileTable(connection, table.getSchema(), table.getTable(), options));
        return result;
    }
}
