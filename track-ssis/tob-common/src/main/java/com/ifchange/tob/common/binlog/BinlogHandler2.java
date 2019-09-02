package com.ifchange.tob.common.binlog;

import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.impl.event.QueryEvent;
import com.ifchange.tob.common.helper.StringHelper;

import java.util.Optional;

// MySQLConstants.QUERY_EVENT
public class BinlogHandler2 extends BinlogHandlerX {
    @Override
    public Optional<BinlogRecord> process(BinlogOperations operations, BinlogEventV4 eventV4) {
        BinlogRecord<String> record = BinlogRecord.newborn(eventV4, true);
        QueryEvent event = (QueryEvent)eventV4;
        record.notes = event.getSql().toString().trim();

        record.database = event.getDatabaseName().toString();
        record.edl = analysisEDL(record.notes);
        record.table = analysisTable(record.edl, record.notes);
        return Optional.ofNullable(record);
    }

    // 分析操作
    private BinlogFactory.EDL analysisEDL(final String src) {
        String sql = src.toUpperCase();
        if (sql.startsWith("INSERT")) {
            return BinlogFactory.EDL.INSERT;
        } else if (sql.startsWith("UPDATE")) {
            return BinlogFactory.EDL.UPDATE;
        } else if (sql.startsWith("DELETE")) {
            return BinlogFactory.EDL.DELETE;
        } else if (sql.startsWith("REPLACE")) {
            return BinlogFactory.EDL.REPLACE;
        } else if (sql.startsWith("CREATE") && sql.substring(6).trim().startsWith("TABLE")) {
            return BinlogFactory.EDL.CREATE_t;
        } else if (sql.startsWith("CREATE") && sql.substring(6).trim().startsWith("DATABASE")) {
            return BinlogFactory.EDL.CREATE_db;
        } else if (sql.startsWith("ALTER")) {
            return BinlogFactory.EDL.ALTER;
        } else if (sql.startsWith("DROP") && sql.substring(4).trim().startsWith("TABLE")) {
            return BinlogFactory.EDL.DROP_t;
        } else if (sql.startsWith("DROP") && sql.substring(4).trim().startsWith("DATABASE")) {
            return BinlogFactory.EDL.DROP_db;
        } else if (sql.startsWith("RENAME")) {
            return BinlogFactory.EDL.RENAME;
        } else if (sql.startsWith("TRUNCATE")) {
            return BinlogFactory.EDL.TRUNCATE;
        } else if (sql.startsWith("BEGIN")) {
            return BinlogFactory.EDL.BEGIN;
        } else if (sql.startsWith("COMMIT")) {
            return BinlogFactory.EDL.COMMIT;
        } else if (sql.startsWith("CREATE") && sql.substring(6).trim().startsWith("USER")) {
            return BinlogFactory.EDL.CREATE_u;
        } else if (sql.startsWith("DROP") && sql.substring(4).trim().startsWith("USER")) {
            return BinlogFactory.EDL.DROP_u;
        } else if (sql.startsWith("GRANT")) {
            return BinlogFactory.EDL.GRANT;
        } else if (sql.startsWith("REVOKE")) {
            return BinlogFactory.EDL.REVOKE;
        }
        return BinlogFactory.EDL.UNDEFINED;
    }

    // 分析表名
    private String analysisTable(BinlogFactory.EDL edl, final String sql) {
        String[] src = sql.split("\\s+");
        // INSERT | INSERT INTO | REPLACE | REPLACE INTO
        if (BinlogFactory.EDL.INSERT == edl || BinlogFactory.EDL.REPLACE == edl) {
            return ("INTO".equalsIgnoreCase(src[1]) ? src[2] : src[1]).replaceAll("[`'\";(]", "").trim();
        }
        // UPDATE t_user | TRUNCATE t_user
        else if (BinlogFactory.EDL.UPDATE == edl || BinlogFactory.EDL.TRUNCATE == edl) {
            return src[1].replaceAll("[`'\";(]", "").trim();
        }
        // DELETE FROM t_user | CREATE TABLE t_user | ALTER TABLE t_user | DROP TABLE t_user | RENAME TABLE t_user
        else if (BinlogFactory.EDL.DELETE == edl
                || BinlogFactory.EDL.CREATE_t == edl
                || BinlogFactory.EDL.ALTER == edl
                || BinlogFactory.EDL.DROP_t == edl
                || BinlogFactory.EDL.RENAME == edl) {
            return src[2].replaceAll("[`'\";(]", "").trim();
        } else {
            return StringHelper.EMPTY;
        }
    }
}
