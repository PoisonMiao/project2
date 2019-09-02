package com.ifchange.tob.common.cluster;

import com.ifchange.tob.common.helper.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
public interface IClusterSrvListener {
    Logger LOG = LoggerFactory.getLogger(IClusterSrvListener.class);
    default void changed(Map<String, List<SrvInstance>> srvGroupMap) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("now online srv list {}", JsonHelper.toJSONString(srvGroupMap));
        }
    }
}
