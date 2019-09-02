package com.ifchange.tob.common.binlog;

import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.impl.event.WriteRowsEventV2;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Optional;

// MySQLConstants.WRITE_ROWS_EVENT_V2
public class BinlogHandler30 extends BinlogHandlerX {
    @Override
    public Optional<BinlogRecord> process(BinlogOperations operations, BinlogEventV4 eventV4) {
        BinlogRecord<List<BinlogRecord.RowD>> record = BinlogRecord.newborn(eventV4, false);
        record.notes = Lists.newArrayList();
        WriteRowsEventV2 event = (WriteRowsEventV2)eventV4;
        BinlogHandler23.doInsertEvent(operations, record, event.getTableId(), event.getRows());
        return Optional.ofNullable(record);
    }
}
