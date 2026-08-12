package eu.openaire.observatory.indicator.survey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.statistics.StatsToolProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure, network-free: asserts the named-query DSL shape confirmed live against
 * http://dl123.madgik.di.uoa.gr:8080/stats-api and observatory-ui's DataService (e.g.
 * getQuestion6()/getQuestion7()) — a different shape from OpenAireQueryBuilder's filter-tree DSL.
 */
class SurveyStatsToolQueryBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SurveyStatsToolQueryBuilder builder =
        new SurveyStatsToolQueryBuilder(objectMapper, new StatsToolProperties().setSurveyProfile("eosc-obs"));

    @Test
    void surveyQuestionSeriesRequestBuildsNamedQueryWithProfile() throws Exception {
        String json = builder.surveyQuestionSeriesRequest(2023, "Question6");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("verbose").asBoolean()).isTrue();
        JsonNode query = root.path("series").get(0).path("query");
        assertThat(query.path("name").asText()).isEqualTo("eosc.sb.2023.Question6");
        assertThat(query.path("profile").asText()).isEqualTo("eosc-obs");
    }

    @Test
    void surveyQuestionSeriesRequestInterpolatesYearAndQuestionCode() throws Exception {
        String json = builder.surveyQuestionSeriesRequest(2022, "Question6.3");

        JsonNode query = objectMapper.readTree(json).path("series").get(0).path("query");
        assertThat(query.path("name").asText()).isEqualTo("eosc.sb.2022.Question6.3");
    }

    @Test
    void surveyQuestionSeriesRequestHasExactlyOneSeries() throws Exception {
        String json = builder.surveyQuestionSeriesRequest(2023, "Question56");

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("series").size()).isEqualTo(1);
    }
}
