package com.ifchange.tob.common.redis;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ifchange.tob.common.core.CacheSerialize;
import com.ifchange.tob.common.helper.BytesHelper;
import com.ifchange.tob.common.helper.CollectsHelper;
import com.ifchange.tob.common.helper.JsonHelper;
import javafx.util.Pair;
import org.apache.ibatis.cache.CacheException;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;
import redis.clients.util.Pool;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Redis 缓存操作类 **/
public class RedisOperations {
    private static final String VAL_KEY = "val", TYPE_KEY = "vt";

    private final boolean cluster;
    private final JedisCluster jedisC;
    private final Pool<? extends JedisCommands> pool;

    RedisOperations(Pool<? extends JedisCommands> pool, JedisCluster jedisC) {
        this.pool = pool;
        this.jedisC = jedisC;
        this.cluster = null == pool;
    }

    /** 写入缓存 **/
    public <T> void put(final String key, final T val, int expire) {
        JedisCommands redis = null;
        try {
            redis = redisCommands();
            if(null != val) {
                setRedisVal(redis, key, expire, RedisValue.newborn(val));
            }
        } finally {
            if(!cluster && null != redis) {
                BytesHelper.close((Closeable)redis);
            }
        }
    }
    /** 读取单个缓存数据 **/
    public <T> T one(final String key, Class<T> clazz) {
        JedisCommands redis = null;
        try {
            redis = redisCommands();
            return oneGet(key, clazz, redis);
        } finally {
            if(!cluster && null != redis) {
                BytesHelper.close((Closeable)redis);
            }
        }
    }

    /** 读取多个缓存数据 **/
    public <T> List<T> multi(final Set<String> keys, Class<T> clazz) {
        List<T> listT = Lists.newArrayList();
        if(!CollectsHelper.isNullOrEmpty(keys)) {
            JedisCommands redis = null;
            try {
                redis = redisCommands();
                for(String key: keys) {
                    T dt = oneGet(key, clazz, redis);
                    if(dt != null) { listT.add(dt); }
                }
            } finally {
                if(!cluster && null != redis) {
                    BytesHelper.close((Closeable)redis);
                }
            }
        }
        return listT;
    }
    /** 读取集合缓存数据 **/
    public <T> List<T> list(final String key, Class<T> clazz) {
        JedisCommands redis = null;
        try {
            redis = redisCommands();
            Map<String, String> rvMap = redis.hgetAll(key);
            if (!CollectsHelper.isNullOrEmpty(rvMap) && rvMap.containsKey(TYPE_KEY) && rvMap.containsKey(VAL_KEY)) {
                if (RedisValue.VT.Collection.name().equals(rvMap.get(TYPE_KEY))) {
                    String serializeKey = serializableKey(rvMap.get(VAL_KEY));
                    return JsonHelper.parseArray(redis.get(serializeKey), clazz);
                }
                throw new CacheException("The key=" + key + " cache is collection please use one method...");
            } else {
                return Lists.newArrayList();
            }
        } finally {
            if(!cluster && null != redis) {
                BytesHelper.close((Closeable)redis);
            }
        }
    }

    /** 清除缓存 **/
    public int clear(String ...keys) {
        int deletes = 0;
        if(CollectsHelper.isNullOrEmpty(keys)) {
            return deletes;
        }
        JedisCommands redis = null;
        try {
            redis = redisCommands();
            for(String key: keys) {
                Map<String, String> rvMap = redis.hgetAll(key);
                deletes += redis.del(key);
                if(!RedisValue.VT.Primitive.name().equals(rvMap.get(TYPE_KEY))) {
                    redis.del(serializableKey(rvMap.get(VAL_KEY)));
                }
            }
        } finally {
            if(!cluster && null != redis) {
                BytesHelper.close((Closeable)redis);
            }
        }
        return deletes;
    }

    /** Multi 类的缓存使用， 返回未中缓存的KEY和结果列表 **/
    public Pair<Set<String>, List<Object>> multiGet(final List<String> keys, Class<?> clazz) {
        List<Object> listT = Lists.newArrayList();
        if(!CollectsHelper.isNullOrEmpty(keys)) {
            JedisCommands redis = null;
            try {
                redis = redisCommands();
                Set<String> overflow = Sets.newHashSet();
                for(String key: keys) {
                    Object dt = oneGet(key, clazz, redis);
                    if(dt != null) {
                        listT.add(dt);
                    } else {
                        overflow.add(key);
                    }
                }
                return new Pair<>(overflow, listT);
            } finally {
                if(!cluster && null != redis) {
                    BytesHelper.close((Closeable)redis);
                }
            }
        }
        return new Pair<>(Sets.newHashSet(keys), listT);
    }

    private <T> T oneGet(String key, Class<T> clazz, JedisCommands redis) {
        Map<String, String> rvMap = redis.hgetAll(key);
        if(!CollectsHelper.isNullOrEmpty(rvMap) && rvMap.containsKey(TYPE_KEY) && rvMap.containsKey(VAL_KEY)) {
            if (RedisValue.VT.Primitive.name().equals(rvMap.get(TYPE_KEY))) {
                return JsonHelper.parseObject(rvMap.get(VAL_KEY), clazz);
            }
            if (RedisValue.VT.Composite.name().equals(rvMap.get(TYPE_KEY))) {
                String serializeKey = serializableKey(rvMap.get(VAL_KEY));
                return JsonHelper.parseObject(redis.get(serializeKey), clazz);
            }
            throw new CacheException("The key=" + key + " cache is collection please use list method...");
        } else {
            return null;
        }
    }

    // 设置缓存
    private void setRedisVal(JedisCommands redis, String key, int expire, RedisValue rv) {
        switch (rv.vt) {
            case Primitive:
                redis.hmset(key, ImmutableMap.of(VAL_KEY, JsonHelper.toJSONString(rv.val), TYPE_KEY, rv.vt.name()));
                break;
            case Composite:
            case Collection:
                String serializeId = ((CacheSerialize)rv.val).getSerializableId();
                redis.hmset(key, ImmutableMap.of(VAL_KEY, serializeId, TYPE_KEY, rv.vt.name()));
                String serializeKey = serializableKey(serializeId);
                redis.set(serializeKey, JsonHelper.toJSONString(rv.val));
                setExpire(redis, serializeKey, expire);
                break;
        }
        setExpire(redis, key, expire);
    }
    // 设置过期时间
    private void setExpire(JedisCommands redis, String key, int expire) {
        if(expire > 0) {
            redis.expire(key, expire);
        }
    }

    boolean isOk() {
        return cluster ? null != jedisC : null != pool;
    }

    private String serializableKey(String serializableId) {
        return "@objects:" + serializableId;
    }

    private JedisCommands redisCommands() {
        if(cluster) {
            return jedisC;
        } else synchronized (pool) {
            JedisCommands jedis = null;
            for (int tries = 0; tries < 3; tries++) {
                try {
                    jedis = pool.getResource();
                    if (null != jedis) {
                        break;
                    }
                } catch (Exception e) {
                    try {
                        TimeUnit.SECONDS.sleep(1L);
                    } catch (InterruptedException ex) {
                        // continue;
                    }
                    if (2 == tries) {
                        throw new RedisException("Get jedis resource error ", e);
                    }
                }
            }
            if (null != jedis) {
                return jedis;
            } else {
                throw new RedisException("Could not get a resource from the pool...");
            }
        }
    }
    @PreDestroy @SuppressWarnings("unused")
    private void destroy() throws IOException {
        if(null != pool) {
            pool.close();
        }
        if(null != jedisC) {
            jedisC.close();
        }
    }
}
