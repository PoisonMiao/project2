package com.ifchange.tob.common.helper;

import com.alibaba.fastjson.JSONException;
import com.ifchange.tob.common.core.ICodeMSG;
import com.ifchange.tob.common.core.RpcException;
import com.ifchange.tob.common.core.RpcReply;
import com.ifchange.tob.common.support.CommonCode;
import com.ifchange.tob.common.support.IConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public final class ExceptionHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHelper.class);
    private static final String X_ERROR_CODE = "x-error-icm";
    private static final String X_ERROR_MSG = "x-error-msg";

    private ExceptionHelper() {
    }
    /** 将错误转写入自定义HEADER中 **/
    public static void responseWrite(final HttpServletResponse response, final Throwable error) {
        RpcException ex;
        //直接抛出 RpcException
        if(error instanceof RpcException) {
            ex = (RpcException) error;
        }
        //请求方法错误
        else if(error instanceof HttpRequestMethodNotSupportedException) {
            final String method = ((HttpRequestMethodNotSupportedException) error).getMethod();
            ex = new RpcException(ICodeMSG.create(405, String.format("接口不支持%s请求方式", method)));
        }
        //请求Content-Type错误
        else if(error instanceof HttpMediaTypeNotSupportedException) {
            HttpMediaTypeNotSupportedException hmE = (HttpMediaTypeNotSupportedException) error;
            final MediaType media = hmE.getContentType(); List<MediaType> types = hmE.getSupportedMediaTypes();
            ex = new RpcException(ICodeMSG.create(415, String.format("接口不支持 Content-Type: [%s] ，请使用 %s", media, types)));
        }
        // 参数校验异常 MethodArgumentNotValidException
        else if(error instanceof MethodArgumentNotValidException) {
            StringBuilder sbMSG = new StringBuilder();
            List<ObjectError> errors = ((MethodArgumentNotValidException) error).getBindingResult().getAllErrors();
            for(ObjectError oe: errors) {
                sbMSG.append(String.format(" | 字段[%s]-%s", ((FieldError)oe).getField(), oe.getDefaultMessage()));
            }
            ex = new RpcException(ICodeMSG.create(402, sbMSG.substring(3)));
        }
        // 参数不匹配异常
        else if (error instanceof ServletRequestBindingException) {
            ex = new RpcException(ICodeMSG.create(420, error.getMessage()));
        }
        //缺少BODY参数
        else if(error instanceof HttpMessageNotReadableException) {
            ex = new RpcException(ICodeMSG.create(419, "请求BODY体内容缺失"));
        }
        // JSON数据不符合接口参数
        else if(error instanceof JSONException) {
            LOG.error("JSON error={}", error.getMessage());
            ex = new RpcException(ICodeMSG.create(421, "请求参数无法解析，请仔细检查JSON数据格式/值/类型"));
        }
        //服务内部未知错误
        else {
            ex = new RpcException(CommonCode.SvError, error);
        }
        if (null != ex.cause) {
            LOG.error("responseBody error ", ex.cause);
        }
        response.setHeader(X_ERROR_CODE, String.valueOf(ex.code()));
        response.setHeader(X_ERROR_MSG, ex.msg());
    }
    /** 判断中是否有错误信息 **/
    public static boolean hasError(final HttpServletResponse response) {
        int status =  response.getStatus();
        return !StringHelper.isBlank(response.getHeader(X_ERROR_CODE)) || status < 200 || status >= 400;
    }
    /** 重置 HttpServletResponse 中的错误信息 **/
    public static void resetResponseError(final HttpServletResponse response) {
        response.setHeader(X_ERROR_CODE, null);
        response.setHeader(X_ERROR_MSG, null);
    }
    /** 获取 HttpServletResponse 需要的 ContentType **/
    public static String contentType(HttpServletRequest request) {
        if(NetworkHelper.isJsonp(request)) {
            return utf8ContentType(MediaType.TEXT_PLAIN_VALUE);
        }
        if(NetworkHelper.needReplyJson(request)) {
            return utf8ContentType(MediaType.APPLICATION_JSON_VALUE);
        }
        return utf8ContentType(MediaType.TEXT_HTML_VALUE);
    }
    /** Throwable 信息序列化 **/
    public static String stringify(Throwable ex) {
        if(null == ex) {
            return StringHelper.EMPTY;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        try {
            ex.printStackTrace(pw);
            return sw.toString();
        }finally {
            pw.flush();
            sw.flush();
            BytesHelper.close(pw);
            BytesHelper.close(sw);
        }
    }
    /** 设置服务处理消耗时长 **/
    public static void setTimeSpentHeader(HttpServletResponse httpResponse) {
        long inTime = Long.parseLong(httpResponse.getHeader(IConstant.X_IN_TIME));
        httpResponse.setHeader(IConstant.X_IN_TIME, null);
        httpResponse.setHeader(timeSpentHeader(), String.valueOf(DateHelper.time() - inTime));
    }
    /** 获取异常信息BODY体 **/
    public static byte[] body(final HttpServletRequest request, final HttpServletResponse response) {
        ICodeMSG mc = ICodeMSG.create(Integer.parseInt(response.getHeader(X_ERROR_CODE)), response.getHeader(X_ERROR_MSG));
        if(NetworkHelper.needReplyJson(request)) {
            return JsonHelper.toJSONBytes(RpcReply.say(mc.code(), mc.msg(), null));
        }
        return BytesHelper.utf8Bytes(EP.replace("${ICON}", ICON).replace("${IMG}", IMG).replace("${D}", mc.message()));
    }
    private static String timeSpentHeader(){
        return String.format("X-%s-Time-Spent", StringHelper.defaultIfBlank(SpringHelper.applicationName(), "SN"));
    }

    private static String utf8ContentType(String contentVal) {
        return contentVal + ";charset=UTF-8";
    }

    private static final String EP = "<html><head><title>ERROR</title><link rel=\"icon\" href=\"${ICON}\"><meta name=\"viewport\" content=\"initial-scale=1,maximum-scale=1,minimum-scale=1,user-scalable=no\"></head><style type=\"text/css\">.container{display:flex;align-items:center;justify-content:center;flex-direction:column;padding-top:30%}.tips{width:66%;margin-top:10px}@media screen and (min-width:460px){.container{padding-top:10%}.tips{width:321}img{width:444px;height:232px;margin-right: 25px;}}</style><body style=\"padding:0;margin:0;background-color:#b8bbc6\"><div class=\"container\"><img src=\"${IMG}\" alt=\"\" width=\"100%\"><div class=\"tips\"><div class=\"title\" style=\"font-size:20px;margin-bottom:10px;font-family:Times,Songti SC;\">呀，不小心出错啦 !！!</div><span style=\"font-size:15px;color:#666\"><span style=\"font-size:18px\">ERROR：</span> ${D}</span></div></div></body></html>";

    private static final String ICON = "data:image/webp;base64,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";

    private static final String IMG = "data:image/webp;base64,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";
}
