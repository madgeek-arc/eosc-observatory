/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.Set;

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
