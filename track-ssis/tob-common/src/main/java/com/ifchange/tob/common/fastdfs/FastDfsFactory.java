package com.ifchange.tob.common.fastdfs;

import com.google.common.collect.Maps;

import java.util.Map;

public final class FastDfsFactory {
    static final Map<String, FastDfsOperations> DFS_MAP = Maps.newConcurrentMap();

    public static FastDfsOperations get(String dfsId) {
        return DFS_MAP.get(dfsId);
    }

    private FastDfsFactory() {}
}
