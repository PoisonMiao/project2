package com.ifchange.tob.common.binlog;

import com.google.code.or.binlog.BinlogEventListener;
import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.BinlogEventV4Header;
import com.google.code.or.binlog.impl.event.RotateEvent;
import com.google.code.or.common.util.MySQLConstants;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

// MySQLConstants Event
public abstract class BinlogHandlerX {
    protected final Logger LOG;

    protected BinlogHandlerX() {
        LOG = LoggerFactory.getLogger(this.getClass());
    }

    abstract Optional<BinlogRecord> process(BinlogOperations operations, BinlogEventV4 eventV4);

    static class DefaultH extends BinlogHandlerX {
        @Override
        Optional<BinlogRecord> process(BinlogOperations operations, BinlogEventV4 eventV4) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Default Parser binlog event: {}", eventV4.toString());
            }
            return Optional.empty();
        }
    }

    static final class ListenerX implements BinlogEventListener {
        private static final Logger LOG = LoggerFactory.getLogger(ListenerX.class);
        private final Map<Integer, BinlogHandlerX> eventMap = Maps.newHashMap();
        private volatile String binlogName;
        private final IBinlogHandler handler;
        private final BinlogOperations operations;
        public ListenerX(BinlogOperations operations, String binlogName, IBinlogHandler handler) {
            this.handler = handler;
            this.operations = operations;
            this.binlogName = binlogName;
        }

        @Override
        public void onEvents(BinlogEventV4 eventV4) {
            BinlogEventV4Header header = eventV4.getHeader();
            long position = header.getPosition(), next = header.getNextPosition();
            try {
                int eventType = header.getEventType();
                // binlog 文件切换事件
                if (MySQLConstants.ROTATE_EVENT == eventType){
                    final RotateEvent event = (RotateEvent) eventV4;
                    this.binlogName = event.getBinlogFileName().toString();
                    position = 0L; next = event.getBinlogPosition();
                } else {
                    BinlogHandlerX handlerX = eventMap.get(eventType);
                    if (null == handlerX) {
                        synchronized (eventMap) {
                            handlerX = eventMap.get(eventType);
                            if (null == handlerX) {
                                try {
                                    Class<?> clzE = Class.forName(BinlogHandlerX.class.getName().replace("X", (eventType + "")));
                                    handlerX = (BinlogHandlerX) clzE.newInstance();
                                } catch (Exception e) {
                                    LOG.warn("Not found custom event handler so using default, {}", e.getMessage());
                                    handlerX = new BinlogHandlerX.DefaultH();
                                }
                                eventMap.put(eventType, handlerX);
                            }
                        }
                    }
                    try {
                        Optional<BinlogRecord> optional = handlerX.process(operations, eventV4);
                        optional.ifPresent(record -> { if (null != handler) handler.piping(record); });
                    } catch (Exception e) {
                        LOG.error("Process binlog event error ", e);
                    }
                }
            } finally {
                if (null != handler && next > 0) {
                    handler.remind(binlogName, position, next);
                }
            }
        }
    }
}
