package com.ifchange.tob.common.helper;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;
import com.ifchange.tob.common.core.RemoteReply;
import javafx.util.Pair;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** HTTP 请求工具类 **/
public final class HttpHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HttpHelper.class);
    private static final MediaType JSON_TYPE = MediaType.parse("application/json;charset=UTF-8");
    private static final MediaType STRING_TYPE = MediaType.parse("text/html;charset=UTF-8");
    private static final MediaType MULTI_PART_TYPE = MediaType.parse("multipart/form-data;charset=UTF-8");
    private static final String PAIR_S = "=", CON_S = "&", UTF8 = "UTF-8";
    private static final int DEFAULT_TIMEOUT = 10;

    /** ok http client */
    private static final OkHttpClient OK_CLIENT = new OkHttpClient().newBuilder()
                                                                    .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                                                                    .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                                                                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                                                                    .addInterceptor(chain -> {
                                                                        Request request = chain.request();
                                                                        if(LOG.isDebugEnabled()) {
                                                                            LOG.debug("ok http request: {}", request.toString());
                                                                        }
                                                                        return chain.proceed(request);
                                                                    }).build();

    private HttpHelper() {}

    public static MediaType multiPartType() {
        return MULTI_PART_TYPE;
    }
    public static MediaType jsonType() {
        return JSON_TYPE;
    }
    public static MediaType stringType() {
        return STRING_TYPE;
    }
    /** 请求 URL 问号后面的 Query 生成器 */
    public static String ofQuery(final List<Pair<String, String>> queries){
        List<String> queryList = Lists.newArrayList();
        for(Pair<String, String> pair: queries){
            queryList.add(pair.getKey().concat(PAIR_S).concat(pair.getValue()));
        }
        return Joiner.on(CON_S).join(queryList);
    }
    /** RequestBody 请求体生成器 */
    public static RequestBody ofBody(MediaType type, final Object body){
        if(null == body){
            return Util.EMPTY_REQUEST;
        }
        if(body instanceof File){
            return RequestBody.create(type, (File)body);
        }
        if(LOG.isDebugEnabled()) {
            String request;
            if(body instanceof byte[]) {
                request = BytesHelper.string((byte[])body);
            } else if (body instanceof String){
                request = (String) body;
            } else {
                request = JsonHelper.toJSONString(body);
            }
            LOG.debug("ok http request body: {}", request);
        }
        if(body instanceof byte[]) {
            return RequestBody.create(type, (byte[]) body);
        } else if (body instanceof String){
            return RequestBody.create(type, ((String) body).getBytes(Charset.forName(UTF8)));
        } else {
            if (JSON_TYPE.equals(type)) {
                return RequestBody.create(type, JsonHelper.toJSONBytes(body));
            } else {
                throw new UnsupportedOperationException("Unsupported media type...");
            }
        }
    }
    /** Form 表单 请求 RequestBody 体生成器 */
    public static RequestBody ofForm(final List<Pair<String, String>> form){
        FormBody.Builder builder = new FormBody.Builder();
        for(Pair<String, String> pair: form){
            if(!StringHelper.isBlank(pair.getKey())) {
                builder.add(pair.getKey(), StringHelper.defaultString(pair.getValue()));
            }
        }
        RequestBody request =  builder.build();
        if (LOG.isDebugEnabled()) {
            Buffer buffer = new Buffer();
            try {
                request.writeTo(buffer);
                LOG.debug("Request body form={}", buffer.readByteString().utf8());
            } catch (Exception e) {

            } finally {
                buffer.flush();
                buffer.close();
            }
        }
        return request;
    }
    /** 异步 GET 请求 */
    public static void asyncGet(final String url, final List<Pair<String, String>> headers, OkHttpCallBack callBack){
        asyncGet(url, headers, callBack, DEFAULT_TIMEOUT);
    }
    /** 异步 GET 请求 */
    public static void asyncGet(final String url, final List<Pair<String, String>> headers, OkHttpCallBack callBack, int timeout){
        asyncRequest((timeout < 1 ? DEFAULT_TIMEOUT : timeout), url, headers(headers), RequestMethod.GET, null, callBack);
    }
    /** 同步 GET 请求带自定义 HEADER，返回单个对象 */
    public static <T> RemoteReply<T> get(final String url, final List<Pair<String, String>> headers, final Class<T> clazz){
        return get(url, headers, clazz, DEFAULT_TIMEOUT);
    }
    /** 同步 GET 请求带自定义 HEADER，返回单个对象 */
    public static <T> RemoteReply<T> get(final String url, final List<Pair<String, String>> headers, final Class<T> clazz, int timeout){
        return syncRequest((timeout < 1 ? DEFAULT_TIMEOUT : timeout), url, headers, RequestMethod.GET, null, clazz);
    }
    /** 异步 POST 请求带自定义 HEADER, 有 RequestBody 请求体 */
    public static void asyncPost(String url, final List<Pair<String, String>> headers, final RequestBody body, final OkHttpCallBack callBack){
        asyncPost(url, headers, body, callBack, DEFAULT_TIMEOUT);
    }
    /** 异步 POST 请求带自定义 HEADER, 有 RequestBody 请求体 */
    public static void asyncPost(String url, final List<Pair<String, String>> headers, final RequestBody body, final OkHttpCallBack callBack, int timeout){
        asyncRequest((timeout < 1 ? DEFAULT_TIMEOUT : timeout), url, headers(headers), RequestMethod.POST, body(body), callBack);
    }
    /** 同步 POST 请求带自定义 HEADER，有 RequestBody 请求体，返回单个对象 */
    public static <T> RemoteReply<T> post(String url, final List<Pair<String, String>> headers, final RequestBody body, final Class<T> clazz){
        return post(url, headers, body, clazz, DEFAULT_TIMEOUT);
    }
    /** 同步 POST 请求带自定义 HEADER，有 RequestBody 请求体，返回单个对象 */
    public static <T> RemoteReply<T> post(String url, final List<Pair<String, String>> headers, final RequestBody body, final Class<T> clazz, int timeout){
        return syncRequest((timeout < 1 ? DEFAULT_TIMEOUT : timeout), url, headers(headers), RequestMethod.POST, body(body), clazz);
    }
    /** 同步请求返回单个实体 **/
    private static <T> RemoteReply<T> syncRequest(int timeout, final String url, final List<Pair<String, String>> headers, final RequestMethod method, final RequestBody rb, final Class<T> clazz) {
        final SettableFuture<RemoteReply<T>> future = SettableFuture.create();
        asyncRequest(timeout, url, headers(headers), method, rb, (code, hds, body) -> {
            try {
                final String result = body.string();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Http result: {}", result);
                }
                //noinspection unchecked
                future.set(RemoteReply.of(clazz, code, result));
            } catch (IOException e) {
                future.setException(e);
            }
        });
        try {
            return future.get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("sync ok http request error", e);
        }
    }
    /** 异步请求 */
    private static void asyncRequest(int timeout, String url, final List<Pair<String, String>> headers, final RequestMethod method, final RequestBody body, final OkHttpCallBack callBack) {
        if(null == callBack){
            throw new RuntimeException("ok http async send request must provider CallBack");
        }
        buildCall(timeout, url, headers, method, body).enqueue(callBack);
    }

    /** 添加 HEADER 生成 Request.Builder */
    private static Request.Builder builderHeader(final List<Pair<String, String>> headers) {
        Request.Builder builder = new Request.Builder();
        if(null != headers && headers.size() > 0){
            Set<String> headSet = Sets.newHashSet();
            for(Pair<String, String> header: headers){
                //如果用户自己添加了这个Header那么OkHttp处理不了返回的 GZIP Response，不添加 OkHttp会自动处理，问题请看 BridgeInterceptor 100 ～ 112 行
                if("Accept-Encoding".equalsIgnoreCase(header.getKey()) && "gzip".equalsIgnoreCase(header.getValue())){
                    continue;
                }
                if(headSet.contains(header.getKey())) {
                    builder.addHeader(header.getKey(), header.getValue());
                } else {
                    builder.header(header.getKey(), header.getValue());
                }
                headSet.add(header.getKey());
            }
        }
        return builder;
    }
    private static List<Pair<String, String>> headers(List<Pair<String, String>> headers) {
        return null == headers ? Lists.newArrayList() : headers;
    }

    private static RequestBody body(RequestBody body) {
        return null == body ? Util.EMPTY_REQUEST : body;
    }
    /** 生成请求 Call */
    private static Call buildCall(int timeout, String url, final List<Pair<String, String>> headers, final RequestMethod method, final RequestBody body){
        Request.Builder builder = builderHeader(headers);
        switch (method){
            case GET:
                builder.url(url).get();
                break;
            case HEAD:
                builder.url(url).head();
                break;
            case POST:
                builder.url(url).post(body);
                break;
            case PUT:
                builder.url(url).put(body);
                break;
            case PATCH:
                builder.url(url).patch(body);
                break;
            case DELETE:
                builder.url(url).delete(body);
                break;
            case OPTIONS:
            case TRACE:
                throw new RuntimeException("ok http request method:" + method.name() + " error.....");
        }
        if(timeout == DEFAULT_TIMEOUT) {
            return OK_CLIENT.newCall(builder.build());
        }
        return OK_CLIENT.newBuilder()
                        .connectTimeout(timeout, TimeUnit.SECONDS)
                        .writeTimeout(timeout, TimeUnit.SECONDS)
                        .readTimeout(timeout, TimeUnit.SECONDS)
                        .build().newCall(builder.build());
    }
    /** 异步请求 CallBack 类 */
    public interface OkHttpCallBack extends Callback {
        @Override
        default void onFailure(Call call, IOException e) {
            LOG.error("ok http async failure error ", e);
        }

        @Override
        default void onResponse(Call call, Response response) {
            try {
                success(response.code(), response.headers(), response.body());
            } catch (Exception e) {
                LOG.error("Dealing ok http async analysis response error ", e);
            } finally {
                BytesHelper.close(response);
            }
        }

        void success(int code, Headers headers, ResponseBody body);
    }
}
