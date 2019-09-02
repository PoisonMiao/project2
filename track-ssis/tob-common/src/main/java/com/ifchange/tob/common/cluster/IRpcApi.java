package com.ifchange.tob.common.cluster;

import com.ifchange.tob.common.helper.CollectsHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface IRpcApi {
    String srv();
    String uri();
    Class<?> arg();
    default int timeout() {
        return MessageRPC.DEFAULT_TIMEOUT;
    }

    /** 执行 SINGLE 调用 **/
    default <R> RpcResponse<R> invoke(String rid, Object body, Class<R> clz) {
        return invoke(rid, body, Collections.EMPTY_MAP, clz);
    }
    /** 执行 SINGLE 调用 **/
    default <R> RpcResponse<R> invoke(String rid, Object body, Map<String, String> headers, Class<R> clz) {
        assertBody(body); RpcRequest request = RpcRequest.newborn(srv(), uri(), rid, body); ofHeader(headers, request);
        return MessageRPC.single(request, clz, timeout());
    }
    /** 执行 SINGLE 调用 **/
    default void invoke(String rid, Object body, Consumer<List<RpcResponse<String>>> consumer) {
        invoke(rid, body, Collections.EMPTY_MAP, consumer);
    }
    /** 执行 SINGLE 调用 **/
    default void invoke(String rid, Object body, Map<String, String> headers, Consumer<List<RpcResponse<String>>> consumer) {
        assertBody(body); RpcRequest request = RpcRequest.newborn(srv(), uri(), rid, body); ofHeader(headers, request);
        MessageRPC.single(request, consumer, timeout());
    }
    /** 执行 MULTI 调用 **/
    default <R> List<RpcResponse<R>> invokes(String rid, Object body, Class<R> clz) {
        return invokes(rid, body, Collections.EMPTY_MAP, clz);
    }
    /** 执行 MULTI 调用 **/
    default <R> List<RpcResponse<R>> invokes(String rid, Object body, Map<String, String> headers, Class<R> clz) {
        assertBody(body);
        RpcRequest request = RpcRequest.newborn(srv(), uri(), rid, body); ofHeader(headers, request);
        return MessageRPC.multi(request, clz, timeout());
    }
    /** 执行 MULTI 调用 **/
    default void invokes(String rid, Object body, Consumer<List<RpcResponse<String>>> consumer) {
        invokes(rid, body, Collections.EMPTY_MAP, consumer);
    }
    /** 执行 MULTI 调用 **/
    default void invokes(String rid, Object body, Map<String, String> headers, Consumer<List<RpcResponse<String>>> consumer) {
        assertBody(body); RpcRequest request = RpcRequest.newborn(srv(), uri(), rid, body); ofHeader(headers, request);
        MessageRPC.multi(request, consumer, timeout());
    }
    default void ofHeader(Map<String, String> headers, RpcRequest request) {
        if(!CollectsHelper.isNullOrEmpty(headers)) {
            for(Map.Entry<String, String> entry: headers.entrySet()) {
                request.ofHeader(entry.getKey(), entry.getValue());
            }
        }
    }
    default void assertBody(Object body) {
        if(null != body && !arg().isAssignableFrom(body.getClass())) {
            throw new IllegalArgumentException("RPC api expect type " + arg().getName() + " as parameter but " + body.getClass().getName());
        }
    }
    static String name(String srv, String uri) {
        return uri + "@" + srv;
    }
}
