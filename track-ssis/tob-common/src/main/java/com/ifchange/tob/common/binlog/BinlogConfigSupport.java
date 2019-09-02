package com.ifchange.tob.common.binlog;

import com.ifchange.tob.common.helper.SpringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.Map;

public abstract class BinlogConfigSupport {
    private static final Logger LOG = LoggerFactory.getLogger(BinlogConfigSupport.class);

    /** 添加多个 Binlog 操作 **/
    protected abstract void addMultiBinlogOperations(Environment env, Map<String, BinlogOperations> blMap);

    /** BINLOG_URI=jdbc:mysql://{host:port}?user={username}&password={password} **/
    protected BinlogOperations newborn(String uri) {
        LOG.info("elasticsearch uri: {}", uri);
        return new BinlogOperations(uri);
    }

    protected BinlogConfigSupport() {
        addMultiBinlogOperations(SpringHelper.getEnvironment(), BinlogFactory.BO_MAP);
    }
}
