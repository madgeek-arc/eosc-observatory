package eu.openaire.observatory.indicator.result;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorResultSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void totalIsOmittedWhenNullAndPresentWhenSet() {
        var result = new IndicatorResult(null, List.of("country"), List.of(
            new IndicatorDataPoint(Map.of("country", "GR"), 3),
            new IndicatorDataPoint(Map.of("country", "DE"), 2, 10L)),
            new QueryExecutionMetadata(Instant.EPOCH, false));

        JsonNode data = mapper.valueToTree(result).get("data");

        assertThat(data.get(0).has("total")).isFalse();
        assertThat(data.get(1).get("total").asLong()).isEqualTo(10);
    }

    @Test
    void aWholeValueExclusionOmitsDimensionAndMember() {
        var result = new IndicatorResult(null, List.of(), List.of(), null, List.of(), List.of(
            IndicatorExclusion.questionPaused(2025),
            IndicatorExclusion.questionPaused(2025, "investmentArea", "SERVICES")));

        JsonNode exclusions = mapper.valueToTree(result).get("exclusions");

        assertThat(exclusions.get(0).get("period").isTextual()).isTrue();
        assertThat(exclusions.get(0).has("dimension")).isFalse();
        assertThat(exclusions.get(0).has("member")).isFalse();
        assertThat(exclusions.get(1).get("member").asText()).isEqualTo("SERVICES");
        assertThat(exclusions.get(1).get("reason").asText()).isEqualTo("QUESTION_PAUSED");
    }
}
