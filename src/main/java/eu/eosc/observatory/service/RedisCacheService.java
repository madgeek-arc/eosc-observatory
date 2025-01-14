package eu.eosc.observatory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RedisCacheService<K, V> implements CacheService<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);
    private static final String PREFIX = "custom:cache:";

    private final RedisTemplate<String, V> redisTemplate;
    private final ValueOperations<String, V> valueOps;

    public RedisCacheService(RedisTemplate<String, V> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
    }

    private String getKey(K key) {
        return key.toString().startsWith(PREFIX) ? key.toString() : PREFIX + key;
    }

    @SuppressWarnings("unchecked")
    public Set<K> fetchKeys(K pattern) {
        String p = getKey(pattern);
        synchronized (this) {
            logger.trace("Fetching all keys matching '{}'", p);
            return (Set<K>) redisTemplate.keys(p);
        }
    }

    @Override
    public boolean containsKey(K key) {
        String k = getKey(key);
        synchronized (this) {
            Boolean exists = redisTemplate.hasKey(k);
            return exists != null && exists;
        }
    }

    @Override
    public V fetch(K key) {
        String k = getKey(key);
        synchronized (this) {
            logger.debug("Fetching value of key: '{}' from cache.", k);
            return valueOps.get(k);
        }
    }

    @Override
    public V remove(K key) {
        String k = getKey(key);
        synchronized (this) {
            logger.debug("Removing value of key: '{}' from cache.", k);
            return valueOps.getAndDelete(k);
        }
    }

    @Override
    public V save(K key, V value) {
        String k = getKey(key);
        synchronized (this) {
            logger.debug("Updating value of key: '{}' from cache.", k);
            return valueOps.getAndSet(k, value);
        }
    }

}
