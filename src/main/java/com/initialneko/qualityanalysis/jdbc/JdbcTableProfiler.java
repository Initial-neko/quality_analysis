package com.initialneko.qualityanalysis.jdbc;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.AnalysisResult;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.TableMetadata;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.profile.ProfileEngine;
import com.initialneko.qualityanalysis.rule.RuleBinding;
import com.initialneko.qualityanalysis.rule.RuleEngine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class JdbcTableProfiler {
    public TableProfile profile(Connection connection, String schema, String table, ProfileOptions options) throws SQLException {
        return analyze(connection, schema, table, options, Collections.<RuleBinding>emptyList()).getProfile();
    }

    /** Profiles and validates a table in one forward-only ResultSet scan. */
    public AnalysisResult analyze(Connection connection, String schema, String table,
                                  ProfileOptions options, List<RuleBinding> rules) throws SQLException {
        String sql = buildScanSql(schema, table);
        Set<String> primaryKeys;
        try {
            primaryKeys = JdbcMetadataReader.readPrimaryKeyColumns(connection, schema, table);
        } catch (SQLException metadataFailure) {
            primaryKeys = Collections.emptySet();
        }

        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.setFetchSize(options.getFetchSize());
            if (options.getQueryTimeoutSeconds() > 0) statement.setQueryTimeout(options.getQueryTimeoutSeconds());
            rs = statement.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();
            TableMetadata metadata = JdbcMetadataReader.fromResultSet(schema, table, rsmd, primaryKeys);
            ProfileEngine profileEngine = new ProfileEngine(metadata, options);
            RuleEngine ruleEngine = new RuleEngine(metadata, options, rules);

            while (rs.next()) {
                profileEngine.beginRow();
                for (int i = 0; i < metadata.getColumns().size(); i++) {
                    ColumnMetadata column = metadata.getColumns().get(i);
                    Object value = JdbcValueReader.read(rs, column, options);
                    profileEngine.acceptCell(i, value);
                    ruleEngine.acceptCell(i, value);
                }
                profileEngine.endRow();
            }

            TableProfile profile = profileEngine.finish();
            return new AnalysisResult(profile, ruleEngine.finish(profile));
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) { }
            if (statement != null) try { statement.close(); } catch (SQLException ignored) { }
        }
    }

    public String buildScanSql(String schema, String table) {
        return "SELECT * FROM " + SqlTableName.qualified(schema, table);
    }
}
