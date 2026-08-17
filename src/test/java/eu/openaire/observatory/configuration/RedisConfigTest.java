package eu.openaire.observatory.configuration;

import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.SurveyAnswerRevisionsAggregation;
import eu.openaire.observatory.indicator.dimension.DimensionMember;
import eu.openaire.observatory.indicator.dimension.DimensionMembers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the actual production serializer built by RedisConfig.redisTemplate(...) — no real
 * Redis connection needed, RedisTemplate.afterPropertiesSet() only validates wiring. This is the
 * test that would have caught the bug this class's mixIn(...) call fixes: DimensionMembers is a
 * record (implicitly final), and DefaultTyping.NON_FINAL alone never stamps type metadata for
 * final types, so a value written to the cache could never be read back — see RedisConfig's own
 * Javadoc on RequiresRedisTypeInfo for the full explanation.
 */
@ExtendWith(MockitoExtension.class)
class RedisConfigTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    private final RedisConfig config = new RedisConfig();

    @Test
    void dimensionMembersRoundTripsThroughTheRedisSerializer() {
        RedisSerializer<Object> serializer = valueSerializer();
        DimensionMembers original = new DimensionMembers(List.of(
            new DimensionMember("Article", "Article"), new DimensionMember("Book", "Book")
        ));

        byte[] bytes = serializer.serialize(original);
        Object roundTripped = serializer.deserialize(bytes);

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void surveyAnswerRevisionsAggregationStillRoundTrips() {
        RedisSerializer<Object> serializer = valueSerializer();
        var original = new SurveyAnswerRevisionsAggregation(new SurveyAnswer());

        byte[] bytes = serializer.serialize(original);
        Object roundTripped = serializer.deserialize(bytes);

        assertThat(roundTripped).isInstanceOf(SurveyAnswerRevisionsAggregation.class);
    }

    /** Matches production's actual ObjectMapper bean (ObservatoryConfig.objectMapper), built via Jackson2ObjectMapperBuilder — a bare `new ObjectMapper()` lacks the auto-registered modules (e.g. ParameterNamesModule) real beans get, which changes deserialization behavior for unrelated reasons. */
    @SuppressWarnings("unchecked")
    private RedisSerializer<Object> valueSerializer() {
        RedisTemplate<String, Object> template = config.redisTemplate(connectionFactory, Jackson2ObjectMapperBuilder.json().build());
        return (RedisSerializer<Object>) template.getValueSerializer();
    }
}
