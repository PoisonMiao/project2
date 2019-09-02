package com.ifchange.tob.common.binlog;

import com.google.code.or.OpenReplicator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ifchange.tob.common.helper.MathHelper;
import com.ifchange.tob.common.helper.NetworkHelper;
import com.ifchange.tob.common.helper.StringHelper;
import com.ifchange.tob.common.ibatis.DataSourceManager;
import com.ifchange.tob.common.ibatis.MySqlException;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public final class BinlogFactory {
    private static final Map<String, DB> infoDbMap = Maps.newHashMap();

    static final Map<String, BinlogOperations> BO_MAP = Maps.newConcurrentMap();

    // 添加多例 binlog operations
    public static void addMultiBinlogOperations(String uri) {
        if (!BO_MAP.containsKey(uri)) {
            synchronized (BO_MAP) {
                if (!BO_MAP.containsKey(uri)) {
                    BO_MAP.put(uri, new BinlogOperations(uri));
                }
            }
        }
    }
    public static BinlogOperations get(String blId) {
        return BO_MAP.get(blId);
    }

    private BinlogFactory() {
    }

    /*
    public static void main(String[] args) {
        String uri = "jdbc:mysql://192.168.1.201:3306?user=devuser&password=devuser";
        BinlogOperations operations = new BinlogOperations(uri);
        // System.out.println(operations.columnsGet("tobusiness", "account"));
        operations.runningAt("mysql-bin.000626", 1073741669, new IBinlogHandler() {});
    }
    */

    public enum EDL {
        INSERT, UPDATE, DELETE, REPLACE,
        CREATE_t, CREATE_db, ALTER, DROP_t, DROP_db, RENAME, TRUNCATE,
        BEGIN, COMMIT,
        CREATE_u, GRANT, REVOKE, DROP_u,
        UNDEFINED
    }

    /** BINLOG_URI=jdbc:mysql://{host:port}?user={username}&password={password} **/
    static OpenReplicator replicator(String uri) {
        DB db = getDB(uri);
        final OpenReplicator replicator = new OpenReplicator();
        replicator.setHost(db.host);
        replicator.setPort(db.port);
        replicator.setUser(db.user);
        replicator.setPassword(db.password);
        replicator.setServerId(db.serverId);
        return replicator;
    }

    static Connection dbc(String uri) {
        DB db = getDB(uri);
        try {
            return db.ds.getConnection();
        } catch (Exception e) {
            throw new MySqlException("Datasource get connection error ", e);
        }
    }

    static String binlogFormat(String uri) {
        return getDB(uri).binlogFormat;
    }

    private static DB getDB(String uri) {
        DB db = infoDbMap.get(uri);
        if (null == db) synchronized (infoDbMap) {
            db = infoDbMap.get(uri);
            if (null == db) {
                db = new DB(uri);
                infoDbMap.put(uri, db);
            }
        }
        return db;
    }

    private static final class DB implements Serializable {
        private static final String SPLITTER = "&", SEPARATOR = "=", USER = "user", PWD = "password", SPLIT = ":";
        private final int port;
        private final String host;
        private final String user;
        private final int serverId;
        private final String password;
        private final DataSource ds;
        final String binlogFormat;

        /** BINLOG_URI=jdbc:mysql://{host:port}?user={username}&password={password} **/
        private DB(String uri) {
            if (StringHelper.isBlank(uri)) {
                throw new MySqlException("MYSQL uri must not null/empty.....");
            }
            this.serverId = serverId();
            int pathIdx = uri.lastIndexOf("?");
            Map<String, String> snMap = StringHelper.map(uri.substring(pathIdx + 1), SPLITTER, SEPARATOR);
            this.user = snMap.get(USER);
            this.password = snMap.get(PWD);
            String[] location = uri.substring(13, pathIdx).split(SPLIT);
            this.host = location[0];
            this.port = MathHelper.toInt(location[1], 3306);

            this.ds = DataSourceManager.newborn(uri);
            this.binlogFormat = binlogFormatGet(ds);
        }

        private int serverId() {
            String ip = NetworkHelper.machineIP();
            String binaryIp = Long.toBinaryString(NetworkHelper.ip2long(ip));
            int ipLength = binaryIp.length();
            return Integer.valueOf((ipLength > 24 ? binaryIp.substring(ipLength - 24) : binaryIp), 2);
        }

        // 获取 binlog_format 值
        private String binlogFormatGet(DataSource ds) {
            try {
                List<String> formats = Lists.newArrayList();
                Connection conn = ds.getConnection();
                Statement statement = null;
                ResultSet rs = null;
                try {
                    statement = conn.createStatement();
                    rs = statement.executeQuery("show variables like 'binlog_format'");
                    while (rs.next()) {
                        formats.add(rs.getString("Value"));
                    }
                } finally {
                    DataSourceManager.closeSqlResource(conn, statement, rs);
                }
                return formats.get(0);
            } catch (Exception e) {
                throw new MySqlException("binlogFormatGet error ", e);
            }
        }
    }

}
