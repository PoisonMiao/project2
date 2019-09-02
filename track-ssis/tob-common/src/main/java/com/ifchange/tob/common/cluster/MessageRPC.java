package com.ifchange.tob.common.cluster;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ifchange.tob.common.core.ICodeMSG;
import com.ifchange.tob.common.core.RpcException;
import com.ifchange.tob.common.helper.CollectsHelper;
import com.ifchange.tob.common.helper.JsonHelper;
import com.ifchange.tob.common.helper.NetworkHelper;
import com.ifchange.tob.common.helper.SpringHelper;
import com.ifchange.tob.common.helper.StringHelper;
import com.ifchange.tob.common.support.CommonCode;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.util.Buffer;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class MessageRPC {
    private static final ConcurrentMap<String, AtomicLong> invokeTimesMap = Maps.newConcurrentMap();
    static final int DEFAULT_TIMEOUT = 10;
    private static MessageDispatcher dispatcher;
    private static ClusterManager manager;

    MessageRPC(ClusterManager manager, MessageDispatcher dispatcher) {
        MessageRPC.dispatcher = dispatcher;
        MessageRPC.manager = manager;
    }

    /** 发送单服务请求 **/
    public static <R> RpcResponse<R> single(RpcRequest request, Class<R> clazz) {
        return single(request, clazz, DEFAULT_TIMEOUT);
    }
    /** 发送单服务请求 **/
    public static <R> RpcResponse<R> single(RpcRequest request, Class<R> clazz, int timeout) {
        assertMessageRpcArgs((null == request ? StringHelper.EMPTY : request.srv), null == clazz, timeout);
        return castSync(rpcBalance(true, request.srv), request, RequestOptions.SYNC().setTimeout(1000L *timeout), clazz).get(0);
    }
    /** 发送单服务请求 **/
    public static void single(final RpcRequest request, final Consumer<List<RpcResponse<String>>> consumer) {
        single(request, consumer, DEFAULT_TIMEOUT);
    }

    /** 发送单服务请求 **/
    public static void single(final RpcRequest request, final Consumer<List<RpcResponse<String>>> consumer, int timeout) {
        assertMessageRpcArgs((null == request ? StringHelper.EMPTY : request.srv), null == consumer, timeout);
        castAsync(rpcBalance(true, request.srv), request, RequestOptions.ASYNC().setTimeout(1000L *timeout), consumer);
    }
    /** 发送多服务请求 **/
    public static <R> List<RpcResponse<R>> multi(RpcRequest request, Class<R> clazz) {
        return multi(request, clazz, DEFAULT_TIMEOUT);
    }
    /** 发送多服务请求 **/
    public static <R> List<RpcResponse<R>> multi(RpcRequest request, Class<R> clazz, int timeout) {
        assertMessageRpcArgs((null == request ? StringHelper.EMPTY : request.srv), null == clazz, timeout);
        return castSync(rpcBalance(false, request.srv), request, RequestOptions.SYNC().setTimeout(1000L *timeout), clazz);
    }

    /** 发送多服务请求 **/
    public static void multi(final RpcRequest request, final Consumer<List<RpcResponse<String>>> consumer) {
        multi(request, consumer, DEFAULT_TIMEOUT);
    }
    /** 发送多服务请求 **/
    public static void multi(final RpcRequest request, final Consumer<List<RpcResponse<String>>> consumer, int timeout) {
        assertMessageRpcArgs((null == request ? StringHelper.EMPTY : request.srv), null == consumer, timeout);
        castAsync(rpcBalance(false, request.srv), request, RequestOptions.ASYNC().setTimeout(1000L *timeout), consumer);
    }
    /** 所有服务调用次数统计 **/
    public static Map<String, Long> invocations() {
        Map<String, Long> invokeSrvTimes = Maps.newHashMap();
        for(ConcurrentMap.Entry<String, AtomicLong> entry: invokeTimesMap.entrySet()) {
            invokeSrvTimes.put(entry.getKey(), entry.getValue().get());
        }
        return invokeSrvTimes;
    }

    private static <R> List<RpcResponse<R>> castSync(List<Address> as, RpcRequest request, RequestOptions opts, Class<R> clz) {
        try {
            final byte[] data = JsonHelper.toJSONBytes(request);
            RequestOptions opt = opts.setFlags(Message.Flag.DONT_BUNDLE, Message.Flag.NO_FC);
            RspList<String> rspList = dispatcher.castMessage(as, new Buffer(data), opt);
            return messageResponseList(clz, rspList);
        } catch (Exception e) {
            throw new ClusterException("jgroups-cluster rpc sync error.....", e);
        }
    }
    private static void castAsync(List<Address> as, final RpcRequest request, RequestOptions opts, Consumer<List<RpcResponse<String>>> con) {
        try {
            final byte[] data = JsonHelper.toJSONBytes(request);
            RequestOptions opt = opts.setFlags(Message.Flag.DONT_BUNDLE, Message.Flag.NO_FC);
            CompletableFuture<RspList<String>> futures = dispatcher.castMessageWithFuture(as, new Buffer(data), opt);
            futures.thenAcceptAsync(resList -> con.accept(messageResponseList(String.class, resList)));
        } catch (Exception e) {
            throw new ClusterException("jgroups-cluster rpc async error.....", e);
        }
    }
    private static <R> List<RpcResponse<R>> messageResponseList(Class<R> clz, RspList<String> rsList) {
        List<RpcResponse<R>> responseList = Lists.newArrayList();
        for(Map.Entry<Address, Rsp<String>> entry: rsList.entrySet()) {
            if(entry.getValue().wasReceived()) {
                RpcResponse.Value value = JsonHelper.parseObject(entry.getValue().getValue(), RpcResponse.Value.class);
                responseList.add(new RpcResponse<>(manager.toSrvInstance(entry.getKey()), clz, value.code, value.data));
            } else {
                RpcResponse.Value value = RpcResponse.val(CommonCode.Timeout.code(), CommonCode.Timeout.msg());
                responseList.add(new RpcResponse<>(manager.toSrvInstance(entry.getKey()), clz, value.code, value.data));
            }
        }
        return responseList;
    }
    private static List<Address> rpcBalance(boolean single, String srvName){
        List<Address> addresses = Lists.newArrayList();
        // 设置服务调用计数器
        invokeTimesMap.putIfAbsent(srvName, new AtomicLong(0));
        // 排除断路服务
        List<SrvInstance> instances = CollectsHelper.filter(manager.viewSrvMap().get(srvName), si -> 1 == si.routed);
        if(!CollectsHelper.isNullOrEmpty(instances)) {
            // 单服调用
            if(single) {
                // 不允许相同服务之间调用
                if (SpringHelper.applicationName().equals(srvName)) {
                    throw new UnsupportedOperationException("you should not single rpc invoke your same service....");
                } else {
                    long times = invokeTimesMap.get(srvName).incrementAndGet();
                    addresses.add(instances.get((int)(times % instances.size())).address);
                }
            }
            // 多服调用, 排除自身调用
            else if (SpringHelper.applicationName().equals(srvName)) {
                invokeTimesMap.get(srvName).incrementAndGet();
                final long sid = SrvInstance.sid(NetworkHelper.machineIP(), SpringHelper.applicationPort());
                addresses = instances.stream().filter(si -> sid != si.sid).map(si -> si.address).collect(Collectors.toList());
            } else {
                invokeTimesMap.get(srvName).incrementAndGet();
                addresses = CollectsHelper.map(instances, si -> si.address);
            }
        }
        if(CollectsHelper.isNullOrEmpty(addresses)) {
            throw new RpcException(ICodeMSG.create(544, "Not Found rpc service with name=" + srvName));
        }
        return addresses;
    }
    private static void assertMessageRpcArgs(String srv, boolean nullClz, int timeout) {
        if (StringHelper.isBlank(srv) || nullClz || timeout < 1) {
            throw new IllegalArgumentException("message rpc args[srvName, clazz, timeout] not available...");
        }
    }
}
