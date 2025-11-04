/*
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

import java.util.Set;

public interface CacheService<K, V> {


    /**
     * Fetches all keys matching the given pattern.
     *
     * @param pattern the pattern to match
     * @return the Set of keys
     */
    Set<K> fetchKeys(K pattern);

    /**
     * Checks whether a certain key exists within the cache.
     *
     * @param key the key to search for
     * @return boolean
     */
    boolean containsKey(K key);

    /**
     * Fetches the value of the requested key.
     *
     * @param key the key
     * @return the value
     */
    V fetch(K key);

    /**
     * Updates (or adds) the key-value pair.
     *
     * @param key the key
     * @param value the value to save
     * @return the value
     */
    V save(K key, V value);

    /**
     * Removes value based on key and returns it.
     *
     * @param key the key of the value to remove
     * @return value
     */
    V remove(K key);

}
