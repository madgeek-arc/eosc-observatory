package eu.eosc.observatory.service;

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
