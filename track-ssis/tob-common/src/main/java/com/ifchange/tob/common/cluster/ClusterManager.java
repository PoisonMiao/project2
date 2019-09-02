package com.ifchange.tob.common.cluster;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.ifchange.tob.common.core.RpcException;
import com.ifchange.tob.common.helper.BeanHelper;
import com.ifchange.tob.common.helper.BytesHelper;
import com.ifchange.tob.common.helper.CollectsHelper;
import com.ifchange.tob.common.helper.JsonHelper;
import com.ifchange.tob.common.helper.LockingHelper;
import com.ifchange.tob.common.helper.NetworkHelper;
import com.ifchange.tob.common.helper.SpringHelper;
import com.ifchange.tob.common.helper.StringHelper;
import com.ifchange.tob.common.helper.ThreadFactoryHelper;
import com.ifchange.tob.common.support.CommonCode;
import javafx.util.Pair;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.Response;
import org.jgroups.blocks.locking.LockService;
import org.jgroups.util.ExtendedUUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StopWatch;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ClusterManager implements RequestHandler, MembershipListener {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterManager.class);
    private static final ExecutorService GroupSrvES = Executors.newSingleThreadExecutor(ThreadFactoryHelper.threadFactoryOf("srv-cluster"));

    private final ConcurrentMap<String, List<SrvInstance>> srvGroupMap = Maps.newConcurrentMap();
    private final BlockingQueue<View> viewQueue = new ArrayBlockingQueue<>(16);
    private final IClusterSrvListener listener;
    private final MessageDispatcher dispatcher;
    private final IMessageHandler handler;
    private final String clusterName;
    private final JChannel channel;

    ClusterManager(String clusterName, IMessageHandler handler, IClusterSrvListener listener) throws Exception {
        onMonitoringJvmCluster();
        this.listener = listener;
        this.handler = handler;
        this.clusterName = clusterName;
        this.channel = new JChannel(new ClassPathResource("/templates/jgroups-cluster.xml").getInputStream());
        channel.setName(SpringHelper.applicationName() + "@" + NetworkHelper.machineIP() + ":" + SpringHelper.applicationPort());
        channel.addAddressGenerator(() -> {
            ExtendedUUID address = ExtendedUUID.randomUUID();
            for(Map.Entry<String, Object> entry: SrvInstance.create().entrySet()) {
                if(!"address".equals(entry.getKey())) {
                    address.put(entry.getKey(), BytesHelper.utf8Bytes(entry.getValue().toString()));
                }
            }
            return address;
        });
        channel.setDiscardOwnMessages(true);
        this.dispatcher = new MessageDispatcher(channel, this).setMembershipListener(this); join();
    }

    /** 断开集群链接 **/
    public boolean disconnect() {
        return !channel.disconnect().isConnected();
    }
    /** 服务加入集群 **/
    public boolean join() throws Exception {
        boolean joined = channel.isConnected();
        if(!joined) {
            joined = channel.connect(ofClusterName()).isConnected();
        }
        if (joined) {
            new MessageRPC(this, dispatcher) {};
            new LockingHelper(new LockService(channel)) {};
        }
        return joined;
    }
    /** 集群名 **/
    public String clusterName() {
        return channel.getClusterName();
    }
    /** 集群实例数 **/
    public int clusterMembers() {
        return channel.getView().getMembers().size();
    }
    /** 集群中的服务 **/
    public Map<String, List<SrvInstance>> viewSrvMap() {
        return Collections.unmodifiableMap(srvGroupMap);
    }

    @Override
    public void viewAccepted(View view) {
        try {
            viewQueue.put(view);
        } catch (InterruptedException e) {
            LOG.error("channel view change to trigger group cluster srv interrupted");
        }
    }

    @Override
    public Object handle(Message msg) {
        StopWatch watch = new StopWatch(); watch.start();
        RpcRequest request = JsonHelper.parseObject(msg.getBuffer(), RpcRequest.class);
        try {
            return JsonHelper.toJSONString(RpcResponse.val(CommonCode.SuccessOk.code(), handler.accept(request)));
        } catch (Exception e) {
            if(e instanceof RpcException) {
                RpcException exception = (RpcException)e;
                return JsonHelper.toJSONString(RpcResponse.val(exception.code(), exception.msg()));
            }
            LOG.error("srv handle message error ", e);
            String data = StringHelper.defaultIfBlank(e.getMessage(), CommonCode.SvError.msg());
            return JsonHelper.toJSONString(RpcResponse.val(CommonCode.SvError.code(), data));
        } finally {
            watch.stop();
            // TODO: 服务端效率统计
            // 资源： request.srv; request.uri; request.rid;
            // 地址： NetworkHelper.machineIP(); SpringHelper.applicationPort()
            // 耗时： watch.getTotalTimeMillis();
            // 客户端： msg.src();
            // 数据长度： msg.length();
        }
    }

    @Override
    public void handle(Message msg, Response response) {
        response.send(handle(msg), false);
    }

    private void onMonitoringJvmCluster() {
        GroupSrvES.submit(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    View view = viewQueue.take();
                    List<Address> members = view.getMembers();
                    ListMultimap<String, SrvInstance> srvMap = CollectsHelper.group(members, address -> {
                        SrvInstance srv = toSrvInstance(address);
                        return new Pair<>(srv.name, srv);
                    });
                    Set<String> srvNameSet = srvMap.keySet();
                    //replace by view
                    for (String srvName : srvNameSet) {
                        srvGroupMap.put(srvName, srvMap.get(srvName));
                    }
                    //filter remove unknown srv
                    Set<String> groupSet = srvGroupMap.keySet();
                    for (String srvName : groupSet) {
                        if (!srvNameSet.contains(srvName)) {
                            srvGroupMap.remove(srvName);
                        }
                    }
                    listener.changed(srvGroupMap);
                    TaskExecutors.triggerRecalculateHashRing(srvMap.get(SpringHelper.applicationName()));
                } catch (InterruptedException e) {
                    LOG.error("channel view change to calculate group cluster srv interrupted error {}", e.getMessage());
                }
            }
        });
    }
    private String ofClusterName() {
        return SpringHelper.applicationEnv() + "@" + clusterName;
    }

    SrvInstance toSrvInstance(Address address) {
        ExtendedUUID uuid = (ExtendedUUID)address;
        SrvInstance instance = new SrvInstance();
        for(String field: SrvInstance.SRV_FIELD) {
            BeanHelper.setProperty(instance, field, BytesHelper.string(uuid.get(field)));
        }
        instance.address = address;
        return instance;
    }

    @Override
    public void suspect(Address suspected) {}
    @Override
    public void block() {}
    @Override
    public void unblock() {}

    @PreDestroy @SuppressWarnings("unused")
    private void destroy() {
        channel.disconnect();
        channel.close();
        srvGroupMap.clear();
        GroupSrvES.shutdown();
    }
}
