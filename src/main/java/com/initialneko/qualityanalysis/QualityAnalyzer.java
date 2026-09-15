package com.initialneko.qualityanalysis;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.jdbc.JdbcTableProfiler;
import com.initialneko.qualityanalysis.model.TableProfile;
import com.initialneko.qualityanalysis.model.TableRef;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Profile-only facade for V1. */
public final class QualityAnalyzer {
    private final JdbcTableProfiler jdbcTableProfiler = new JdbcTableProfiler();

    public TableProfile profileTable(Connection connection, String schema, String table) throws SQLException {
        return profileTable(connection, schema, table, ProfileOptions.defaults());
    }

    public TableProfile profileTable(Connection connection, String schema, String table, ProfileOptions options) throws SQLException {
        return jdbcTableProfiler.profile(connection, schema, table, options);
    }

    public List<TableProfile> profileTables(Connection connection, List<TableRef> tables, ProfileOptions options) throws SQLException {
        List<TableProfile> result = new ArrayList<TableProfile>(tables.size());
        for (TableRef table : tables) result.add(profileTable(connection, table.getSchema(), table.getTable(), options));
        return result;
    }
}
