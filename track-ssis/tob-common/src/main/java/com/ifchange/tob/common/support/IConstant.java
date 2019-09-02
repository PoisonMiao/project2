package com.ifchange.tob.common.support;

public interface IConstant {
	/** 最小 base64 PNG图片 **/
	String MIN_BASE64_IMAGE = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQImWMoKyv7DwAFKgJi3Cd5fQAAAABJRU5ErkJggg==";
	/** favicon.ico  **/
	String FAVICON_ICON = "/favicon.ico";

	/** 配置KEY将日志写入文件，值为 STDOUT | FILE | STREAM 默认值 STDOUT **/
	String KEY_LOG_APPENDER = "server.log-appender";

	/** 输出异常堆栈日志到控制台 **/
	String KEY_LOG_CAUSE_TRACE_STDOUT = "log.cause-trace-stdout";

	/** 配置日志级别, 默认值 INFO **/
	String KEY_LOG_LEVEL = "server.log-level";

	/** 配置 STREAM 日志的地址 **/
	String KEY_LOG_RABBIT_URI = "server.log-rabbit-uri";
	String KEY_LOG_ELASTIC_URI = "server.log-elastic-uri";

	/** 是否启用操作日志 true/false， 默认值 false **/
	String KEY_OPERATIONS_LOG_ENABLE = "operations.log.enable";

	/** SnowFlake唯一ID生成器配置 ip、hostname 默认值 ip， 若配置hostname 则（hostname规则： xxxx.number 如 ifchange.001）**/
	String KEY_SNOW_MID_GENERATOR = "snow.machine.id.generator";

	/** 是否打印 IBATIS MAPPER true/false, 默认值 false 執行參數 **/
	String KEY_SHOW_IBATIS_SQL_P = "show.ibatis.arg-sql";

	/** 接口返回JSON **/
	String HTTP_REPLY_JSON = "JSON";

	/** JSONP 回调函数参数 @C **/
	String JSONP_CB = "@C";

	/** 无数据 **/
	String DATA_NONE = "N/A";

	/** 访问进入时间 **/
	String X_IN_TIME = "X-IN-TIME";

	/** 日志APPENDER **/
	String APPENDER_STDOUT = "STDOUT";
	String APPENDER_FILE = "FILE";
	String APPENDER_STREAM = "STREAM";

	// 是否为 spring cloud 的端点
	static boolean isSpringCloudEndpoint(String uri) {
		return null != uri && uri.startsWith("/actuator/");
	}
}
