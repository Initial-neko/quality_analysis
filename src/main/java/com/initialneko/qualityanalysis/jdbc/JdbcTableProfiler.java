package com.initialneko.qualityanalysis.jdbc;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.TableMetadata;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.profile.ProfileEngine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Set;

/** Single-pass, profile-only JDBC scanner for V1. */
public final class JdbcTableProfiler {
    public TableProfile profile(Connection connection, String schema, String table, ProfileOptions options) throws SQLException {
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

            while (rs.next()) {
                profileEngine.beginRow();
                for (int i = 0; i < metadata.getColumns().size(); i++) {
                    ColumnMetadata column = metadata.getColumns().get(i);
                    Object value = JdbcValueReader.read(rs, column, options);
                    profileEngine.acceptCell(i, value);
                }
                profileEngine.endRow();
            }
            return profileEngine.finish();
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) { }
            if (statement != null) try { statement.close(); } catch (SQLException ignored) { }
        }
    }

    public String buildScanSql(String schema, String table) {
        return "SELECT * FROM " + SqlTableName.qualified(schema, table);
    }
}
