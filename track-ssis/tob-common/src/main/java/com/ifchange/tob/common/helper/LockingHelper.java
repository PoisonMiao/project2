package com.ifchange.tob.common.helper;

import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.jgroups.blocks.locking.LockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public abstract class LockingHelper {
    private static final Logger LOG = LoggerFactory.getLogger(LockingHelper.class);

    private static final ConcurrentMap<String, LocalLockInfo> LOCAL_LOCK = Maps.newConcurrentMap();
    private static LockService lockService;

    protected LockingHelper(LockService lockService) {
        LockingHelper.lockService = lockService;
    }

    /** 建立多线程锁 **/
    public static boolean tryThreadLock(String name) {
        String lockName = lockName(name);
        if(!LOCAL_LOCK.containsKey(lockName)){
            try {
                LOCAL_LOCK.put(lockName, generateLockInfo(lockName));
                return true;
            } catch (Exception e) {
                LOG.error("Get thread lock={} error={}", name, e.getMessage());
                return false;
            }
        }
        return false;
    }

    /** 释放多线程锁 **/
    public static void releaseThreadLock(String name) {
        String lockName = lockName(name);
        LocalLockInfo lockInfo = LOCAL_LOCK.get(lockName);
        LocalLockInfo threadLockInfo = generateLockInfo(lockName);
        if(threadLockInfo.equals(lockInfo)) {
            long times = 0L;
            //noinspection SingleStatementInBlock
            while (true) {
                try {
                    LOCAL_LOCK.remove(lockName);
                    return;
                } catch (Exception e) {
                    continueDealing();
                }
                times += 100;
                if(times > 1000L) {
                    throw new RuntimeException("release thread lock timeout=1s");
                }
            }
        }
        if(null != lockInfo) {
            LOG.warn("The thread can not release others create lock.....");
        }
    }

    /** 建立集群锁 **/
    public static boolean tryClusterLock(String name) {
        if(null != lockService) {
            Lock lock = lockService.getLock(lockName(name));
            return lock.tryLock();
        }
        throw new UnsupportedOperationException("srv not support cluster lock");
    }
    /** 释放集群锁 **/
    public static void releaseClusterLock(String name) {
        if(null != lockService) {
            Lock lock = lockService.getLock(lockName(name));
            if (null != lock) {
                long times = 0L;
                //noinspection SingleStatementInBlock
                while (true) {
                    try {
                        lock.unlock();
                        return;
                    } catch (Exception e) {
                        continueDealing();
                    }
                    times += 100;
                    if(times > 3000L) {
                        throw new RuntimeException("release cluster lock timeout=3s");
                    }
                }
            }
        }
    }
    private static void continueDealing() {
        try {
            TimeUnit.MILLISECONDS.sleep(100L);
        } catch (InterruptedException e1) {
            // release thread lock
        }
    }
    private static String lockName(String name) {
        return StringHelper.defaultString(name) + "@" + SpringHelper.applicationName();
    }
    private static LocalLockInfo generateLockInfo(String lockName) {
        Thread thread = Thread.currentThread();
        return new LocalLockInfo(thread.getId(), thread.getName(), lockName);
    }
    private static class LocalLockInfo {
        final long localTreadId;
        final String localTreadName;
        final String localLockName;
        private LocalLockInfo(long localTreadId, String localTreadName, String localLockName) {
            this.localTreadId = localTreadId;
            this.localTreadName = localTreadName;
            this.localLockName = localLockName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LocalLockInfo that = (LocalLockInfo) o;
            return new EqualsBuilder()
                    .append(localTreadId, that.localTreadId)
                    .append(localTreadName, that.localTreadName)
                    .append(localLockName, that.localLockName)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(localTreadId)
                    .append(localTreadName)
                    .append(localLockName)
                    .toHashCode();
        }
    }
}
