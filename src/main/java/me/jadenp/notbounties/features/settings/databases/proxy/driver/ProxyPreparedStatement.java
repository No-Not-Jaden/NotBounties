package me.jadenp.notbounties.features.settings.databases.proxy.driver;

import me.jadenp.notbounties.features.settings.databases.proxy.ProxyMessaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.Connection;
import java.sql.Clob;
import java.sql.Date;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class ProxyPreparedStatement implements PreparedStatement {
    private final ProxyConnection connection;
    private final String sql;
    private final Map<Integer, ProxyMessaging.DBParam> params = new HashMap<>();
    private final List<List<ProxyMessaging.DBParam>> batchParams = new ArrayList<>();
    private boolean closed = false;

    ProxyPreparedStatement(ProxyConnection connection, String sql) {
        this.connection = connection;
        this.sql = sql;
    }

    private void ensureOpen() throws SQLException {
        if (closed) throw new SQLException("PreparedStatement is closed");
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private List<ProxyMessaging.DBParam> buildParamList() {
        List<Integer> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        List<ProxyMessaging.DBParam> out = new ArrayList<>(keys.size());
        for (Integer k : keys) out.add(params.get(k));
        return out;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        ensureOpen();
        ProxyMessaging.DBResponse resp = ProxyMessaging.requestExecuteQuery(sql, buildParamList(), 10_000);
        if (resp.error != null) throw new SQLException(resp.error);
        if (resp.rows == null || resp.columns == null) throw new SQLException("No result set returned");
        return ProxyResultSets.create(resp.rows, resp.columns);
    }

    @Override
    public int executeUpdate() throws SQLException {
        ensureOpen();
        ProxyMessaging.DBResponse resp = ProxyMessaging.requestExecuteUpdate(sql, buildParamList(), 10_000);
        if (resp.error != null) throw new SQLException(resp.error);
        return resp.updateCount;
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) {
        params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.NULL, null));
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setByte(int parameterIndex, byte x) { params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.INT, (int)x)); }

    @Override
    public void setShort(int parameterIndex, short x) { params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.INT, (int)x)); }

    @Override
    public void setInt(int parameterIndex, int x) { params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.INT, x)); }

    @Override
    public void setLong(int parameterIndex, long x) { params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.LONG, x)); }

    @Override
    public void setFloat(int parameterIndex, float x) { params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.DOUBLE, (double)x)); }

    @Override
    public void setDouble(int parameterIndex, double x) { params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.DOUBLE, x)); }

    @Override
    public void setBigDecimal(int parameterIndex, java.math.BigDecimal x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setString(int parameterIndex, String x) { params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.STRING, x)); }

    @Override
    public void setBytes(int parameterIndex, byte[] x) { params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.BYTES, x)); }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    // Legacy JDBC method
    @Deprecated
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }

    @Override
    public void clearParameters() { params.clear(); }

    @Override
    public void setObject(int parameterIndex, Object x) { params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.STRING, String.valueOf(x))); }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        try {
            byte[] bytes = x.getBytes(1, (int) x.length());
            params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.BLOB, bytes));
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        try {
            byte[] bytes = toByteArray(inputStream);
            params.put(parameterIndex, new ProxyMessaging.DBParam(parameterIndex, ProxyMessaging.DBParam.Kind.BLOB, bytes));
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public boolean execute() throws SQLException {
        String trimmed = sql.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("select")) {
            executeQuery();
            return true;
        } else {
            executeUpdate();
            return false;
        }
    }

    @Override
    public void addBatch() {
        batchParams.add(buildParamList());
    }

    @Override
    public void clearBatch() { batchParams.clear(); }

    @Override
    public int[] executeBatch() throws SQLException {
        ensureOpen();
        ProxyMessaging.DBResponse resp = ProxyMessaging.requestExecuteBatch(sql, batchParams, 10_000);
        if (resp.error != null) throw new SQLException(resp.error);
        return resp.batchCounts == null ? new int[0] : resp.batchCounts;
    }

    @Override
    public void close() { closed = true; }

    // Unused/unsupported methods
    @Override public ResultSet executeQuery(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int executeUpdate(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int getMaxFieldSize() throws SQLException { return 0; }
    @Override public void setMaxFieldSize(int max) throws SQLException { }
    @Override public int getMaxRows() throws SQLException { return 0; }
    @Override public void setMaxRows(int max) throws SQLException { }
    @Override public void setEscapeProcessing(boolean enable) throws SQLException { }
    @Override public int getQueryTimeout() throws SQLException { return 0; }
    @Override public void setQueryTimeout(int seconds) throws SQLException { }
    @Override public void cancel() throws SQLException { }
    @Override public SQLWarning getWarnings() throws SQLException { return null; }
    @Override public void clearWarnings() throws SQLException { }
    @Override public void setCursorName(String name) throws SQLException { }
    @Override public boolean execute(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public ResultSet getResultSet() throws SQLException { return null; }
    @Override public int getUpdateCount() throws SQLException { return -1; }
    @Override public boolean getMoreResults() throws SQLException { return false; }
    @Override public void setFetchDirection(int direction) throws SQLException { }
    @Override public int getFetchDirection() throws SQLException { return ResultSet.FETCH_FORWARD; }
    @Override public void setFetchSize(int rows) throws SQLException { }
    @Override public int getFetchSize() throws SQLException { return 0; }
    @Override public int getResultSetConcurrency() throws SQLException { return ResultSet.CONCUR_READ_ONLY; }
    @Override public int getResultSetType() throws SQLException { return ResultSet.TYPE_FORWARD_ONLY; }
    @Override public void addBatch(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public Connection getConnection() throws SQLException { return connection; }
    @Override public boolean getMoreResults(int current) throws SQLException { return false; }
    @Override public ResultSet getGeneratedKeys() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public boolean execute(String sql, String[] columnNames) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public int getResultSetHoldability() throws SQLException { return ResultSet.HOLD_CURSORS_OVER_COMMIT; }
    @Override public boolean isClosed() throws SQLException { return closed; }
    @Override public void setPoolable(boolean poolable) throws SQLException { }
    @Override public boolean isPoolable() throws SQLException { return false; }
    @Override public void closeOnCompletion() throws SQLException { }
    @Override public boolean isCloseOnCompletion() throws SQLException { return false; }
    @Override public long getLargeUpdateCount() throws SQLException { return getUpdateCount(); }
    @Override public void setLargeMaxRows(long max) throws SQLException { }
    @Override public long getLargeMaxRows() throws SQLException { return 0; }
    @Override public long[] executeLargeBatch() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public long executeLargeUpdate(String sql) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Not a wrapper"); }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return false; }

    // Remaining setter stubs to satisfy interface
    @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setRef(int parameterIndex, Ref x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setClob(int parameterIndex, Clob x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setArray(int parameterIndex, Array x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public ResultSetMetaData getMetaData() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException { setNull(parameterIndex, sqlType); }
    @Override public void setURL(int parameterIndex, java.net.URL x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public ParameterMetaData getParameterMetaData() throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setRowId(int parameterIndex, RowId x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNString(int parameterIndex, String value) throws SQLException { setString(parameterIndex, value); }
    @Override public void setNCharacterStream(int parameterIndex, java.io.Reader value, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNClob(int parameterIndex, NClob value) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setClob(int parameterIndex, java.io.Reader reader, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException { setBlob(parameterIndex, inputStream); }
    @Override public void setNClob(int parameterIndex, java.io.Reader reader, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setCharacterStream(int parameterIndex, java.io.Reader reader) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setNCharacterStream(int parameterIndex, java.io.Reader value) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    @Override public void setClob(int parameterIndex, java.io.Reader reader) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    public void setNClob(int parameterIndex, NClob value, long length) throws SQLException { throw new SQLFeatureNotSupportedException(); }
    public void setNClob(int parameterIndex, java.io.Reader reader) throws SQLException { throw new SQLFeatureNotSupportedException(); }
}
