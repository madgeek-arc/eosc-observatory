package eu.openaire.observatory.indicator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Indexes a list of values by a key derived from each value, and looks them up by key — shared by
 * {@code IndicatorQueryHandlerRegistry} and {@code DimensionMemberProviderRegistry}, which differ
 * only in what they're keying (handler key vs. dimension code) and what exception they throw when
 * a lookup misses.
 */
public final class KeyedRegistry<K, V> {

    private final Map<K, V> byKey;

    public KeyedRegistry(List<V> values, Function<V, K> keyExtractor) {
        this.byKey = values.stream().collect(Collectors.toUnmodifiableMap(keyExtractor, Function.identity()));
    }

    public V getRequired(K key, Function<K, ? extends RuntimeException> notFoundException) {
        V value = byKey.get(key);
        if (value == null) {
            throw notFoundException.apply(key);
        }
        return value;
    }

    public Optional<V> find(K key) {
        return Optional.ofNullable(byKey.get(key));
    }
}
