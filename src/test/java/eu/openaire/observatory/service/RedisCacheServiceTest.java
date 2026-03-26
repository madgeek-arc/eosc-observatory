package eu.openaire.observatory.service;

import gr.uoa.di.madgik.registry.service.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.SerializationException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    private static final String TEST_KEY = "custom:cache:key";

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisCacheService<String, String> service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new RedisCacheService<>(redisTemplate);
    }

    @Test
    void fetchReturnsCachedValueUsingPrefixedKey() {
        when(valueOperations.get(TEST_KEY)).thenReturn("value");

        String result = service.fetch("key");

        assertEquals("value", result);
        verify(valueOperations).get(TEST_KEY);
    }

    @Test
    void fetchDeletesUnreadableEntriesAndThrowsServiceException() {
        when(valueOperations.get(TEST_KEY)).thenThrow(new SerializationException("broken"));

        assertThrows(ServiceException.class, () -> service.fetch("key"));

        verify(redisTemplate).delete(TEST_KEY);
    }

    @Test
    void removeReturnsExistingValueAndDeletesKey() {
        when(valueOperations.get(TEST_KEY)).thenReturn("existing");

        String removed = service.remove("key");

        assertEquals("existing", removed);
        verify(redisTemplate).delete(TEST_KEY);
    }

    @Test
    void removeStillDeletesUnreadableEntries() {
        when(valueOperations.get(TEST_KEY)).thenThrow(new SerializationException("broken"));

        String removed = service.remove("key");

        assertNull(removed);
        verify(redisTemplate).delete(TEST_KEY);
    }

    @Test
    void saveOverwritesEntryEvenWhenOldValueCannotBeDeserialized() {
        when(valueOperations.get(TEST_KEY)).thenThrow(new SerializationException("broken"));

        String previous = service.save("key", "new-value");

        assertNull(previous);
        verify(valueOperations).set(TEST_KEY, "new-value");
    }

    @Test
    void fetchKeysPassesPrefixedPatternToRedis() {
        when(redisTemplate.keys("custom:cache:sa-*")).thenReturn(Set.of("custom:cache:sa-1"));

        Set<String> keys = service.fetchKeys("sa-*");

        assertEquals(Set.of("custom:cache:sa-1"), keys);
        verify(redisTemplate).keys("custom:cache:sa-*");
    }
}
