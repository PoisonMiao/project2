package com.ifchange.tob.common.view.parser;

import com.ifchange.tob.common.helper.MathHelper;
import com.ifchange.tob.common.helper.NetworkHelper;
import com.ifchange.tob.common.helper.SnowIdHelper;
import com.ifchange.tob.common.helper.StringHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

public final class RequestSession implements Serializable {
    private static final long serialVersionUID = 5384867717637634816L;
    private static final String X_User_Agent = "User-Agent";
    // 访问ID
    public String rid;

    // 访问的域名
    public String domain;

    // 请求方式
    public String method;

    // 访问的 URI
    public String uri;

    // QUERY 数据
    public String query;

    // UserAgent
    public String userAgent;

    // 应用版本
    public String vNo;

    // 设备编号
    public String deviceNo;

    // 请求时的客户端时间
    public Long clientTime;

    // 客户端IP
    public String clientIp;

    // 请求数据签值
    public String dataSecret;

    // 授权加密值
    public String signature;

    // 用户访问到达时间
    public Long accessTime;

    // BODY 数据
    public String body;

    // 机构租户代码
    public String tid;
    // 用户ID
    public String uid;
    // 扩展数据
    public String ext;

    /** 生成请求认证 Session **/
    public static RequestSession newborn(HttpServletRequest request) {
        RequestSession session = new RequestSession();
        session.rid = StringHelper.defaultIfBlank(header(request, RequestHeader.RID), SnowIdHelper.unique());
        session.accessTime = System.currentTimeMillis();
        session.method = request.getMethod();
        // URL 数据
        session.uri = request.getRequestURI();
        session.query = request.getQueryString();
        session.domain = request.getServerName();
        // HEADER 数据
        session.userAgent = request.getHeader(X_User_Agent);
        session.clientIp = NetworkHelper.ofClientIp(request);
        session.vNo = header(request, RequestHeader.Vno);
        session.deviceNo = header(request, RequestHeader.DeviceID);
        session.signature = header(request, RequestHeader.Signature);
        session.dataSecret = header(request, RequestHeader.DataSecret);
        session.clientTime = MathHelper.toLong(header(request, RequestHeader.ClientTime), 0);
        // Signature 权限签名计算数据
        session.tid = session.uid = session.ext = session.body = StringHelper.EMPTY;
        return session;
    }

    private static String header(HttpServletRequest request, RequestHeader headerKey) {
        return request.getHeader(headerKey.name());
    }
}
