package com.ifchange.tob.common.gearman;

import com.google.common.net.HostAndPort;
import com.ifchange.tob.common.fastdfs.FastDfsException;
import com.ifchange.tob.common.gearman.lib.Gearman;
import com.ifchange.tob.common.helper.MathHelper;
import com.ifchange.tob.common.helper.StringHelper;

import java.util.Map;

public final class GearmanFactory {
    private static final String SPLITTER = "&", SEPARATOR = "=", CORES = "cores", TIMEOUT = "timeout", WORKERS = "workers";

    private GearmanFactory() {
    }

    /**
     * URI=gearman://{SRV}@host:port?core={CORE}&timeout={TIMEOUT}&workers={WORKERS}
     */
    public static Gearman gearman(String uri) {
        if (StringHelper.isBlank(uri) || !uri.startsWith("gearman://")) {
            throw new FastDfsException("Gearman uri must not blank or null and must format as gearman://{SRV}@host:port?core={CORE}&timeout={TIMEOUT}&workers={WORKERS} ");
        }
        try {
            int qmIdx = uri.indexOf("?");
            Map<String, String> props = StringHelper.map(uri.substring(qmIdx +1), SPLITTER, SEPARATOR);
            int core = MathHelper.toInt(props.get(CORES), 1);
            int timeout = MathHelper.toInt(props.get(TIMEOUT), 5);
            int workers = MathHelper.toInt(props.get(WORKERS), 2);
            String[] gName = uri.substring(10, qmIdx).split("@");
            return new Gearman(gName[0], HostAndPort.fromString(gName[1]), core, timeout, workers);
        } catch (Exception e) {
            throw new FastDfsException("FastDFS init TrackerClient by uri=" + uri + " error...", e);
        }
    }
}
