package com.ifchange.tob.common.support;

import com.alibaba.fastjson.annotation.JSONField;
import com.ifchange.tob.common.esearch.orm.Property;
import com.ifchange.tob.common.esearch.orm.Typical;
import com.ifchange.tob.common.helper.JsonHelper;

import java.io.Serializable;

/** 操作日志 **/
public class OperationLog implements Serializable {
	private static final long serialVersionUID = -1792967204418671858L;
	@Property(type = Typical.Long, desc = "日志ID")
	public long logId;

	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "请求ID")
	public String rid;

	@JSONField(ordinal = 1)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "API接口名")
	public String apiName;

	@Property(type = Typical.Double, desc = "访问的时间")
	@JSONField(ordinal = 2, format = "#.000")
	public Double accessTime;

	@JSONField(ordinal = 3)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "接口URI")
	public String uri;

	@JSONField(ordinal = 4)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "接口QUERY数据")
	public String query;

	@JSONField(ordinal = 5)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "访问的域名")
	public String domain;

	@JSONField(ordinal = 6)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "客户端IP")
	public String clientIp;

	@JSONField(ordinal = 7)
	@Property(type = Typical.Text, desc = "客户端UserAgent")
	public String userAgent;

	@JSONField(ordinal = 8)
	@Property(type = Typical.Text, desc = "授权加密值")
	public String signature;

	@JSONField(ordinal = 9)
	@Property(type = Typical.Text, desc = "BODY参数")
	private Object requestBody;

	@JSONField(ordinal = 10)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "用户ID")
	public String uid;

	@JSONField(ordinal = 11)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "机构租户代码")
	public String tid;

	@JSONField(ordinal = 12)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "服务名")
	public String sn;

	@JSONField(ordinal = 13)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "环境")
	public String env;

	@JSONField(ordinal = 14)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "服务HOST")
	public String host;

	@JSONField(ordinal = 15)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "服务IP")
	public String serverIp;

	@JSONField(ordinal = 16)
	@Property(type = Typical.Long, desc = "接口耗时")
	public Long cost;

	@JSONField(ordinal = 17)
	@Property(type = Typical.Keyword, analyzer = "not_analyzed", desc = "操作状态： true-成功， false-失败")
	public boolean status;

	@JSONField(ordinal = 18)
	@Property(type = Typical.Text, desc = "接口返回数据")
	private Object responseBody;

	public String getRequestBody() {
		return requestBody instanceof String ? (String)requestBody : JsonHelper.toJSONString(requestBody);
	}

	public void setRequestBody(Object requestBody) {
		this.requestBody = requestBody;
	}

	public String getResponseBody() {
		return responseBody instanceof String ? (String)responseBody : JsonHelper.toJSONString(responseBody);
	}

	public void setResponseBody(Object responseBody) {
		this.responseBody = responseBody;
	}
}
