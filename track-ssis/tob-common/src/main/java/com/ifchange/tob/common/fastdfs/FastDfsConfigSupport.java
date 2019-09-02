package com.ifchange.tob.common.fastdfs;

import com.ifchange.tob.common.helper.SpringHelper;
import org.springframework.core.env.Environment;

import java.util.Map;

public abstract class FastDfsConfigSupport {
    /** 添加多个FastDFS操作 **/
    protected abstract void addMultiFastDfsOperations(Environment env, Map<String, FastDfsOperations> dfsMap);

    /**
     * URI = dfs://host:port,host:port,host:port?ct={CT}&nt={NT}&thp={THP}&hast={HAST}&hsk={HSK}&charset={CHARSET}
     * host:port 对应参数 tracker_server
     * ct 对应参数 connect_timeout
     * nt 对应参数 network_timeout
     * thp 对应参数 http.tracker_http_port
     * hast 对应参数 http.anti_steal_token
     * hsk 对应参数 http.secret_key
     */
    protected FastDfsOperations newborn(final String uri) {
        return new FastDfsOperations(uri);
    }

    protected FastDfsConfigSupport() {
        addMultiFastDfsOperations(SpringHelper.getEnvironment(), FastDfsFactory.DFS_MAP);
    }
}
