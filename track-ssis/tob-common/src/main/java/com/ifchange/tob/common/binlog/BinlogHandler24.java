package com.ifchange.tob.common.binlog;

import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.impl.event.UpdateRowsEvent;
import com.google.code.or.common.glossary.Row;
import com.google.common.collect.Lists;
import javafx.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// MySQLConstants.UPDATE_ROWS_EVENT
public class BinlogHandler24 extends BinlogHandlerX {
    @Override
    public Optional<BinlogRecord> process(BinlogOperations operations, BinlogEventV4 eventV4) {
        BinlogRecord<List<BinlogRecord.RowD>> record = BinlogRecord.newborn(eventV4, false);
        record.notes = Lists.newArrayList();
        UpdateRowsEvent event = (UpdateRowsEvent)eventV4;
        doUpdateEvent(operations, record, event.getTableId(), event.getRows());
        return Optional.ofNullable(record);
    }

    static void doUpdateEvent(BinlogOperations operations, BinlogRecord<List<BinlogRecord.RowD>> record, long tableId, List<com.google.code.or.common.glossary.Pair<Row>> rows) {
        Pair<String, String> dbTable = BinlogHandler19.dbTable(tableId);
        record.edl = BinlogFactory.EDL.UPDATE;
        record.database = dbTable.getKey();
        record.table = dbTable.getValue();
        for(com.google.code.or.common.glossary.Pair<Row> pair: rows) {
            Map<String, Object> before = operations.rowMap(pair.getBefore().getColumns(), record.database, record.table);
            Map<String, Object> after = operations.rowMap(pair.getAfter().getColumns(), record.database, record.table);
            record.notes.add(new BinlogRecord.RowD(before, after));
        }
    }
}
