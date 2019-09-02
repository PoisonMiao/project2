package com.ifchange.tob.common.binlog;

import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.impl.event.TableMapEvent;
import com.google.common.collect.Maps;
import javafx.util.Pair;

import java.util.Map;
import java.util.Optional;
// MySQLConstants.TABLE_MAP_EVENT
// 每次ROW_EVENT前都伴随一个TABLE_MAP_EVENT事件，保存一些表信息，如tableId, tableName, databaseName, 而ROW_EVENT只有tableId
public class BinlogHandler19 extends BinlogHandlerX {
    private static Map<Long, Pair<String, String>> tableMap = Maps.newConcurrentMap();

    @Override
    Optional<BinlogRecord> process(BinlogOperations operations, BinlogEventV4 eventV4) {
        final TableMapEvent event = (TableMapEvent)eventV4;
        long tableId = event.getTableId();
        tableMap.remove(tableId);
        tableMap.put(tableId, new Pair<>(event.getDatabaseName().toString(), event.getTableName().toString()));
        return Optional.empty();
    }

    static Pair<String, String> dbTable(long tableId) {
        return tableMap.get(tableId);
    }
}
