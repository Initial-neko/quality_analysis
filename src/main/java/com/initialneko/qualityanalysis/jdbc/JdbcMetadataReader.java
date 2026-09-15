package com.initialneko.qualityanalysis.jdbc;

import com.initialneko.qualityanalysis.model.ColumnMetadata;
import com.initialneko.qualityanalysis.model.TableMetadata;
import com.initialneko.qualityanalysis.util.JdbcTypeFamilies;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class JdbcMetadataReader {
    private JdbcMetadataReader() {}

    static Set<String> readPrimaryKeyColumns(Connection connection, String schema, String table) throws SQLException {
        Set<String> result = new HashSet<String>();
        DatabaseMetaData md = connection.getMetaData();
        ResultSet rs = null;
        try {
            rs = md.getPrimaryKeys(connection.getCatalog(), schema, table);
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                if (name != null) result.add(name.toUpperCase(Locale.ROOT));
            }
        } finally {
            if (rs != null) rs.close();
        }
        return result;
    }

    static TableMetadata fromResultSet(String schema, String table, ResultSetMetaData rsmd, Set<String> primaryKeys) throws SQLException {
        int count = rsmd.getColumnCount();
        List<ColumnMetadata> columns = new ArrayList<ColumnMetadata>(count);
        for (int i = 1; i <= count; i++) {
            String name = rsmd.getColumnName(i);
            String label = rsmd.getColumnLabel(i);
            int jdbcType = rsmd.getColumnType(i);
            String typeName = rsmd.getColumnTypeName(i);
            boolean nullable = rsmd.isNullable(i) != ResultSetMetaData.columnNoNulls;
            boolean pk = name != null && primaryKeys.contains(name.toUpperCase(Locale.ROOT));
            columns.add(new ColumnMetadata(i, name, label, jdbcType, typeName,
                    rsmd.getPrecision(i), rsmd.getScale(i), nullable, pk, JdbcTypeFamilies.familyOf(jdbcType)));
        }
        return new TableMetadata(schema, table, columns);
    }
}
