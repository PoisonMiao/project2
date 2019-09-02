package com.ifchange.tob.common.binlog;

import com.clearspring.analytics.util.Lists;
import com.google.code.or.OpenReplicator;
import com.google.code.or.common.glossary.Column;
import com.google.code.or.common.glossary.column.StringColumn;
import com.google.common.collect.Maps;
import com.ifchange.tob.common.helper.CollectsHelper;
import com.ifchange.tob.common.helper.StringHelper;
import com.ifchange.tob.common.ibatis.DataSourceManager;
import com.ifchange.tob.common.ibatis.MySqlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BinlogOperations {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Logger LOG = LoggerFactory.getLogger(BinlogOperations.class);
    private static Map<String, List<String>> columnsMap = Maps.newHashMap();

    private final String uri;
    private final OpenReplicator replicator;

    public BinlogOperations(String uri) {
        this.uri = uri;
        this.replicator = BinlogFactory.replicator(uri);
    }

    // 启动 binlog 同步
    public void runningAt(String binlogName, long position, IBinlogHandler handler) {
        try {
            replicator.setBinlogFileName(binlogName);
            replicator.setBinlogPosition(position < 4 ? 4 : position);
            replicator.setBinlogEventListener(new BinlogHandlerX.ListenerX(this, binlogName, handler));
            replicator.start();
        } catch (Exception e){
            throw new MySqlException("BinlogOperations.runningAt error ", e);
        }
    }

    // 获取所有 binlog
    public List<String> binlogsGet() {
        Connection connection = BinlogFactory.dbc(uri);
        Statement state = null; ResultSet rs = null;
        try {
            List<String> binlogList = Lists.newArrayList();
            state = connection.createStatement();
            rs = state.executeQuery("show binary logs");
            while(rs.next()){
                binlogList.add(rs.getString("Log_name"));
            }
            return binlogList;
        } catch (Exception e) {
            throw new MySqlException("binlogsGet error ", e);
        } finally{
            DataSourceManager.closeSqlResource(connection, state, rs);
        }
    }

    // 获取 binlog_format
    public String binlogFormat() {
        return BinlogFactory.binlogFormat(uri);
    }

    // 停止 binlog 同步
    public void stop(long timeout, TimeUnit unit) {
        if (null != replicator) {
            replicator.stopQuietly(timeout, unit);
        }
    }

    // 判断 binlog 同步是否在运行中
    public boolean isRunning() {
        return null != replicator && replicator.isRunning();
    }

    Map<String, Object> rowMap(List<Column> row, String db, String table) {
        Map<String, Object> rowMap = Maps.newHashMap();
        if (null != row && !row.isEmpty()) {
            List<String> columns = columnsGet(db, table);
            if (columns.size() != row.size()) {
                columns = columnsForceGet(db, table);
            }
            if (columns.size() == row.size()) {
                for (int idx = 0; idx < columns.size(); idx++) {
                    rowMap.put(columns.get(idx), columnVal(row.get(idx)));
                }
            } else {
                LOG.warn("DB={}, TABLE={} columns is not equal to row.getColumns()...");
            }
        }
        return rowMap;
    }

    private Object columnVal(Column column) {
        if (column instanceof StringColumn) {
            return string(((StringColumn)column).getValue());
        }
        return column.getValue();
    }

    // 获取 columns 信息
    private List<String> columnsGet(String dbn, String table) {
        String key = dbn + "." + table;
        List<String> columns = columnsMap.get(key);
        if (null == columns) synchronized (columnsMap) {
            columns = columnsMap.get(key);
            if (null == columns) {
                columns = getColumns(dbn, table);
                columnsMap.put(key, columns);
            }
        }
        return columns;
    }
    private synchronized List<String> columnsForceGet(String dbn, String table) {
        List<String> columns = getColumns(dbn, table);
        columnsMap.put((dbn + "." + table), columns);
        return columns;
    }
    private List<String> getColumns(String dbn, String table) {
        Connection connection = BinlogFactory.dbc(uri);
        try {
            return CollectsHelper.map(DataSourceManager.columnsGet(connection, dbn, table), tc -> tc.column);

        } catch (Exception e) {
            throw new MySqlException("getColumns error ", e);
        } finally {
            DataSourceManager.closeSqlResource(connection, null);
        }
    }

    private static String string(final byte[] source) {
        return null == source ? StringHelper.EMPTY : new String(source, UTF8);
    }

    @PreDestroy @SuppressWarnings("unused")
    private void destroy() {
        stop(100, TimeUnit.MILLISECONDS);
    }
}
