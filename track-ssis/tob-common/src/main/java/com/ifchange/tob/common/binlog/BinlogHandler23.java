package com.ifchange.tob.common.binlog;

import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.impl.event.WriteRowsEvent;
import com.google.code.or.common.glossary.Row;
import com.google.common.collect.Lists;
import javafx.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// MySQLConstants.WRITE_ROWS_EVENT
public class BinlogHandler23 extends BinlogHandlerX {
    @Override
    public Optional<BinlogRecord> process(BinlogOperations operations, BinlogEventV4 eventV4) {
        BinlogRecord<List<BinlogRecord.RowD>> record = BinlogRecord.newborn(eventV4, false);
        record.notes = Lists.newArrayList();
        WriteRowsEvent event = (WriteRowsEvent)eventV4;
        doInsertEvent(operations, record, event.getTableId(), event.getRows());
        return Optional.ofNullable(record);
    }

    static void doInsertEvent(BinlogOperations operations, BinlogRecord<List<BinlogRecord.RowD>> record, long tableId, List<Row> rows) {
        Pair<String, String> dbTable = BinlogHandler19.dbTable(tableId);
        record.edl = BinlogFactory.EDL.INSERT;
        record.database = dbTable.getKey();
        record.table = dbTable.getValue();
        for(Row row: rows) {
            Map<String, Object> after = operations.rowMap(row.getColumns(), record.database, record.table);
            record.notes.add(new BinlogRecord.RowD(null, after));
        }
    }
}
