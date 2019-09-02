package com.ifchange.tob.common.fastdfs;

import com.ifchange.tob.common.helper.BytesHelper;
import com.ifchange.tob.common.helper.StringHelper;
import com.ifchange.tob.common.support.PoolConfig;
import javafx.util.Pair;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.TrackerClient;

import java.net.URLDecoder;
import java.util.Map;
import java.util.Properties;

public final class FastDfsHelper {
    private static final String SPLITTER = "&", SEPARATOR = "=", CT = "ct", NT = "nt", THP = "thp", HAST = "hast", HSK = "hsk", CHARSET = "charset";
    public static final String SPLIT_GROUP_NAME_AND_FILENAME_SEPARATOR = "/";
    private static final int POOL = 128;

    /**
     * URI = dfs://host:port,host:port,host:port?ct={CT}&nt={NT}&thp={THP}&hast={HAST}&hsk={HSK}&charset={CHARSET}
     * host:port 对应参数 tracker_server
     * ct 对应参数 connect_timeout
     * nt 对应参数 network_timeout
     * thp 对应参数 http.tracker_http_port
     * hast 对应参数 http.anti_steal_token
     * hsk 对应参数 http.secret_key
     */
    public static TrackerClient trackerClient(String uri) {
        if (StringHelper.isBlank(uri) || !uri.startsWith("dfs://")) {
            throw new FastDfsException("FastDFS uri must not blank or null and must format as dfs://host:port,host:port?ct={CT}&nt={NT}&thp={THP}&hast={HAST}&hsk={HSK}&charset={CHARSET} ");
        }
        try {
            int qmIdx = uri.indexOf("?"); Properties properties = new Properties();
            properties.put(ClientGlobal.PROP_KEY_TRACKER_SERVERS, uri.substring(6, qmIdx));
            Map<String, String> props = StringHelper.map(uri.substring(qmIdx +1), SPLITTER, SEPARATOR);
            String connectTimeout = StringHelper.defaultIfBlank(props.get(CT), String.valueOf(ClientGlobal.DEFAULT_CONNECT_TIMEOUT));
            properties.put(ClientGlobal.PROP_KEY_CONNECT_TIMEOUT_IN_SECONDS, connectTimeout);
            String networkTimeout = StringHelper.defaultIfBlank(props.get(NT), String.valueOf(ClientGlobal.DEFAULT_NETWORK_TIMEOUT));
            properties.put(ClientGlobal.PROP_KEY_NETWORK_TIMEOUT_IN_SECONDS, networkTimeout);
            String httpPort = StringHelper.defaultIfBlank(props.get(THP), String.valueOf(ClientGlobal.DEFAULT_HTTP_TRACKER_HTTP_PORT));
            properties.put(ClientGlobal.PROP_KEY_HTTP_TRACKER_HTTP_PORT, httpPort);
            String stealToken = StringHelper.defaultIfBlank(props.get(HAST), String.valueOf(ClientGlobal.DEFAULT_HTTP_ANTI_STEAL_TOKEN));
            properties.put(ClientGlobal.PROP_KEY_HTTP_ANTI_STEAL_TOKEN, stealToken);
            String secretKey = StringHelper.defaultIfBlank(props.get(HSK), ClientGlobal.DEFAULT_HTTP_SECRET_KEY);
            properties.put(ClientGlobal.PROP_KEY_HTTP_SECRET_KEY, URLDecoder.decode(secretKey, BytesHelper.UTF8.name()));
            String charset = StringHelper.defaultIfBlank(props.get(CHARSET), ClientGlobal.DEFAULT_CHARSET);
            properties.put(ClientGlobal.CONF_KEY_CHARSET, charset);
            ClientGlobal.initByProperties(properties);
            return new TrackerClient();
        } catch (Exception e) {
            throw new FastDfsException("FastDFS init TrackerClient by uri=" + uri + " error...", e);
        }
    }

    /**
     * URI = dfs://host:port,host:port,host:port?ct={CT}&nt={NT}&thp={THP}&hast={HAST}&hsk={HSK}&charset={CHARSET}
     * host:port 对应参数 tracker_server
     * ct 对应参数 connect_timeout
     * nt 对应参数 network_timeout
     * thp 对应参数 http.tracker_http_port
     * hast 对应参数 http.anti_steal_token
     * hsk 对应参数 http.secret_key
     */
    public static StorageClientPool storageClientPool(String uri) {
        StorageClientCreator creator = new StorageClientCreator(uri);
        return new StorageClientPool(creator, PoolConfig.of(POOL));
    }

    public static Pair<String, String> splitFileId(String fileId) {
        int pos = fileId.indexOf(SPLIT_GROUP_NAME_AND_FILENAME_SEPARATOR);
        if ((pos <= 0) || (pos == fileId.length() - 1)) {
            throw new FastDfsException("You provide an error fileId=" + fileId);
        }
        return new Pair<>(fileId.substring(0, pos), fileId.substring(pos + 1));
    }
}
