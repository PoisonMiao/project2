package com.ifchange.tob.common.cluster;

import com.google.common.collect.Sets;
import com.ifchange.tob.common.helper.CollectsHelper;
import com.ifchange.tob.common.helper.HashingHelper;
import com.ifchange.tob.common.helper.NetworkHelper;
import com.ifchange.tob.common.helper.SpringHelper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Consumer;

public final class TaskExecutors {
    private TaskExecutors() {}
    private static LinkedHashSet<SrvInstance> BEFORE_INSTANCES;
    private static volatile SortedMap<Integer, SrvInstance> HASH_RING;
    /** JVM集群服务去中心化任务处理 **/
    public static <T> boolean executed(String taskId, T parameter, Consumer<T> consumer) {
        if(null == HASH_RING) {
            consumer.accept(parameter);
            return true;
        }
        SrvInstance instance = HashingHelper.targetNode(taskId, HASH_RING);
        // 只有一致性HASH 落在本机上的任务才做 consumer 处理，否则跳过
        if(SpringHelper.applicationPort().equals(instance.port) && NetworkHelper.machineIP().equals(instance.ip)) {
            consumer.accept(parameter);
            return true;
        }
        return false;
    }

    //集群中本服务相同的实例有变动时重新计算HASH环
    static void triggerRecalculateHashRing(List<SrvInstance> instances) {
        if(!CollectsHelper.isNullOrEmpty(instances)) {
            LinkedHashSet<SrvInstance> nowInstanceSet = Sets.newLinkedHashSet(instances);
            if (!isSetEquals(nowInstanceSet, BEFORE_INSTANCES)) {
                BEFORE_INSTANCES = nowInstanceSet;
                HASH_RING = HashingHelper.makeHashRing(BEFORE_INSTANCES, 32);
            }
        } else {
            HASH_RING = null;
        }
    }
    //比较两个SET是否相同
    private static boolean isSetEquals(Set<SrvInstance> siSet1, Set<SrvInstance> siSet2) {
        // both are null
        if (siSet1 == null && siSet2 == null) {
            return true;
        }
        if (siSet1 == null || siSet2 == null || siSet1.size() != siSet2.size() || siSet1.size() == 0 || siSet2.size() == 0) {
            return false;
        }
        //所有实例相同
        for(SrvInstance si: siSet2) {
            if(!siSet1.contains(si)) {
                return false;
            }
        }
        return true;
    }
}
