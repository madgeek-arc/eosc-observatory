package eu.openaire.observatory.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.domain.Action;
import eu.openaire.observatory.domain.Editor;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.Revision;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.SurveyAnswerRevisionsAggregation;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins the Redis round trip for {@link SurveyAnswerRevisionsAggregation}.
 *
 * <p>{@code RedisCacheServiceTest} mocks {@code RedisTemplate}, so nothing else in the suite proves
 * a real aggregate survives {@code GenericJackson2JsonRedisSerializer}. That matters because
 * fetch → mutate → save is the production edit path <em>and</em> what the purge cache scrub does,
 * and the class has two {@code final} fields with no setters ({@code editors}, {@code created}) plus
 * a constructor for new editing sessions whose body appends a history entry.
 *
 * <p>The serializer here is configured exactly as {@code RedisConfig#redisTemplate} configures it.
 */
class RevisionsCacheRoundTripTest {

    private static final GenericJackson2JsonRedisSerializer SERIALIZER = serializer();

    private static GenericJackson2JsonRedisSerializer serializer() {
        ObjectMapper redisMapper = Jackson2ObjectMapperBuilder.json().build();
        redisMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        redisMapper.activateDefaultTyping(
                redisMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return new GenericJackson2JsonRedisSerializer(redisMapper);
    }

    @Test
    void aggregationSurvivesTheRedisRoundTrip() {
        SurveyAnswerRevisionsAggregation original = aggregationWithOneEdit();

        SurveyAnswerRevisionsAggregation restored = roundTrip(original);

        assertNotNull(restored);
        assertEquals("sa-1", restored.getSurveyAnswer().getId());
        assertEquals(1, restored.getEditors().size());
        assertEquals("editor@example.org", restored.getEditors().getFirst().getUser());
        assertEquals(1, restored.getRevisions().size());
    }

    @Test
    void roundTripPreservesHistory() {
        SurveyAnswerRevisionsAggregation original = aggregationWithOneEdit();
        int before = original.getSurveyAnswer().getHistory().getEntries().size();

        SurveyAnswerRevisionsAggregation restored = roundTrip(original);

        assertEquals(before, restored.getSurveyAnswer().getHistory().getEntries().size());
    }

    /**
     * {@code created} drives the ten-minute age check in {@code SurveyAnswerCrudService#autoSaveCache}.
     * If it reset to "now" on every fetch, {@code active} would never exceed the threshold and the
     * cache would never flush.
     */
    @Test
    void roundTripPreservesCreated() throws InterruptedException {
        SurveyAnswerRevisionsAggregation original = aggregationWithOneEdit();
        // Put a measurable gap between construction and the round trip: `created` is a final field
        // initialised to `new Date()`, so without this the assertion could pass merely because both
        // values landed in the same millisecond.
        Thread.sleep(5);

        SurveyAnswerRevisionsAggregation restored = roundTrip(original);

        assertEquals(original.getCreated(), restored.getCreated());
    }

    private SurveyAnswerRevisionsAggregation roundTrip(SurveyAnswerRevisionsAggregation aggregation) {
        return (SurveyAnswerRevisionsAggregation) SERIALIZER.deserialize(SERIALIZER.serialize(aggregation));
    }

    private SurveyAnswerRevisionsAggregation aggregationWithOneEdit() {
        SurveyAnswer surveyAnswer = new SurveyAnswer();
        surveyAnswer.setId("sa-1");
        surveyAnswer.setMetadata(new Metadata());
        surveyAnswer.getMetadata().setModifiedBy("editor@example.org");
        surveyAnswer.setAnswer(new JSONObject());

        SurveyAnswerRevisionsAggregation aggregation = new SurveyAnswerRevisionsAggregation(surveyAnswer);

        Revision revision = new Revision();
        revision.setField("section.question");
        revision.setValue("answer");
        revision.setAction(new Action().setType(Action.Type.ADD));

        aggregation.applyRevision(revision, new Editor()
                .setUser("editor@example.org")
                .setRole("manager")
                .setUpdateDate(new Date(10_000L)));

        return aggregation;
    }
}
