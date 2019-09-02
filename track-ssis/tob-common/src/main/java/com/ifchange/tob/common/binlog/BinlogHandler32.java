package com.ifchange.tob.common.binlog;

import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.impl.event.DeleteRowsEventV2;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Optional;

// MySQLConstants.DELETE_ROWS_EVENT_V2
public class BinlogHandler32 extends BinlogHandlerX {
    @Override
    public Optional<BinlogRecord> process(BinlogOperations operations, BinlogEventV4 eventV4) {
        BinlogRecord<List<BinlogRecord.RowD>> record = BinlogRecord.newborn(eventV4, false);
        record.notes = Lists.newArrayList();
        DeleteRowsEventV2 event = (DeleteRowsEventV2) eventV4;
        BinlogHandler25.doDeleteEvent(operations, record, event.getTableId(), event.getRows());
        return Optional.ofNullable(record);
    }
}
