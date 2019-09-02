package com.ifchange.tob.common.binlog;

import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.BinlogEventV4Header;
import com.google.common.collect.Maps;
import com.ifchange.tob.common.helper.JsonHelper;

import java.io.Serializable;
import java.util.Map;

public class BinlogRecord<BR> implements Serializable {
    BinlogFactory.EDL edl;
    String database;
    String table;
    BR notes;
    private Integer event;
    private Long timestamp;
    private Long receiptTime;
    private boolean noteSQL;

    static <N> BinlogRecord<N> newborn(BinlogEventV4 eventV4, boolean noteSQL) {
        BinlogEventV4Header header = eventV4.getHeader();
        BinlogRecord<N> record = new BinlogRecord<>();
        record.noteSQL = noteSQL;
        record.event = header.getEventType();
        record.timestamp = header.getTimestamp();
        record.receiptTime = header.getTimestampOfReceipt();
        return record;
    }

    public BinlogFactory.EDL getEdl() {
        return edl;
    }
    public String getDatabase() {
        return database;
    }
    public String getTable() {
        return table;
    }
    public BR getNotes() {
        return notes;
    }
    public Integer getEvent() {
        return event;
    }
    public Long getTimestamp() {
        return timestamp;
    }
    public Long getReceiptTime() {
        return receiptTime;
    }

    public boolean isNoteSQL() {
        return noteSQL;
    }

    @Override
    public String toString() {
        return JsonHelper.toJSONString(this);
    }

    public static final class RowD implements Serializable {
        private static final long serialVersionUID = -3793396189220591525L;

        public final Map<String, Object> before;
        public final Map<String, Object> after;
        RowD(Map<String, Object> before, Map<String, Object> after) {
            this.before = (null == before ? Maps.newHashMap() : before);
            this.after = (null == after ? Maps.newHashMap() : after);
        }
    }
}
