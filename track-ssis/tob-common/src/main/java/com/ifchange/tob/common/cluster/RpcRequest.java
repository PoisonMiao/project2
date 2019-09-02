package com.ifchange.tob.common.cluster;

import com.google.common.collect.Maps;
import com.ifchange.tob.common.helper.DateHelper;
import com.ifchange.tob.common.helper.JsonHelper;
import com.ifchange.tob.common.helper.StringHelper;

import java.io.Serializable;
import java.util.Map;

public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 5462364139562342421L;
    /** 请求时间 **/
    public long time;
    /** 请求服务 **/
    public String srv;
    /** 资源定位 **/
    public String uri;
    /** 请求标识 **/
    public String rid;
    /** 请求数据 **/
    public byte[] body;
    /** 请求头数据 **/
    public Map<String, String> headers = Maps.newHashMap();

    /** 生成消息请求对象 **/
    public static RpcRequest newborn(String srv, String uri, String rid, Object data) {
        RpcRequest request = new RpcRequest();
        request.srv = srv;
        request.uri = uri;
        request.rid = rid;
        request.time = DateHelper.time();
        request.body = data instanceof byte[] ? (byte[])data : JsonHelper.toJSONBytes(data);
        return request;
    }
    /** 设置请求头数据 **/
    public RpcRequest ofHeader(String key, String value) {
        if(!StringHelper.isBlank(key) && !StringHelper.isBlank(value)) {
            headers.put(key, StringHelper.defaultString(value));
        }
        return this;
    }

    String stringH() {
        return JsonHelper.toJSONString(headers);
    }

    public String stringB() {
        return StringHelper.defaultString(JsonHelper.parseObject(body, String.class));
    }
}
