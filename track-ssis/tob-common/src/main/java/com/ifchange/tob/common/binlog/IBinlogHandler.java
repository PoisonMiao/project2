package com.ifchange.tob.common.binlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IBinlogHandler {
    Logger LOG = LoggerFactory.getLogger(IBinlogHandler.class);

    default void piping(BinlogRecord record) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("BinlogRecord={}", record.toString());
        }
    }
    default void remind(String binlogName, long position, long next){
        if (LOG.isDebugEnabled()) {
            LOG.debug("binlogName={}, position={}, next={}", binlogName, position, next);
        }
    }
}
