package com.initialneko.qualityanalysis.jdbc;

import com.initialneko.qualityanalysis.config.ProfileOptions;
import com.initialneko.qualityanalysis.model.ColumnMetadata;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

final class JdbcValueReader {
    private JdbcValueReader() {}

    static Object read(ResultSet rs, ColumnMetadata column, ProfileOptions options) throws SQLException {
        int index = column.getOrdinal();
        switch (column.getJdbcType()) {
            case Types.CLOB:
                return readClob(rs.getClob(index), options);
            case Types.NCLOB:
                return readNClob(rs.getNClob(index), options);
            case Types.BLOB:
                return readBlob(rs.getBlob(index));
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
                Object value = rs.getObject(index);
                if (value instanceof NClob) return readNClob((NClob) value, options);
                if (value instanceof Clob) return readClob((Clob) value, options);
                return value;
            default:
                return rs.getObject(index);
        }
    }

    private static Object readClob(Clob clob, ProfileOptions options) throws SQLException {
        if (clob == null) return null;
        try {
            long length = clob.length();
            String preview = null;
            if (options.getClobPreviewChars() > 0 && length > 0) {
                preview = clob.getSubString(1L, (int) Math.min((long) options.getClobPreviewChars(), length));
            }
            return LobValue.clob(length, preview);
        } finally {
            try { clob.free(); } catch (Throwable ignored) { }
        }
    }

    private static Object readNClob(NClob clob, ProfileOptions options) throws SQLException { return readClob(clob, options); }

    private static Object readBlob(Blob blob) throws SQLException {
        if (blob == null) return null;
        try {
            return LobValue.blob(blob.length());
        } finally {
            try { blob.free(); } catch (Throwable ignored) { }
        }
    }
}
