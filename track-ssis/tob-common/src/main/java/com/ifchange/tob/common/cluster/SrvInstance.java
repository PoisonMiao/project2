package com.ifchange.tob.common.cluster;

import com.ifchange.tob.common.helper.BeanHelper;
import com.ifchange.tob.common.helper.NetworkHelper;
import com.ifchange.tob.common.helper.SpringHelper;
import com.ifchange.tob.common.helper.StringHelper;
import org.jgroups.Address;

import java.io.Serializable;
import java.util.Map;

public class SrvInstance implements Serializable {
    private static final long serialVersionUID = -2640584741246680084L;

    /** srv id **/
    public long sid;
    /** 服务名 **/
    public String name;
    /** 服务IP **/
    public String ip;
    /** 服务PORT **/
    public String port;
    /** 服务时间 **/
    public long time;
    /** 可路由服务 **/
    public transient int routed = 1;
    /** JGroups地址 **/
    public transient Address address;


    static Map<String, Object> create() {
        SrvInstance instance = new SrvInstance();
        instance.name = SpringHelper.applicationName();
        instance.ip = NetworkHelper.machineIP();
        instance.port = SpringHelper.applicationPort();
        instance.time = System.currentTimeMillis();
        instance.sid = sid(instance.ip, instance.port);
        return BeanHelper.bean2Map(instance);
    }

    static long sid(String ip, String port) {
        return Long.parseLong((port + StringHelper.EMPTY + NetworkHelper.ip2long(ip)));
    }

    static String[] SRV_FIELD = new String[]{"name", "ip", "port", "time", "sid"};

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("sid=").append(sid);
        sb.append(",name='").append(name).append('\'');
        sb.append(",ip='").append(ip).append('\'');
        sb.append(",port='").append(port).append('\'');
        sb.append(",time=").append(time);
        sb.append('}');
        return sb.toString();
    }
}
