package eu.openaire.observatory.indicator.survey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.openaire.observatory.statistics.StatsToolProperties;
import org.springframework.stereotype.Component;

/**
 * Pure, network-free builder for the survey stats-tool's "raw?json=" DSL — a named-query style
 * ({@code series[].query.name}/{@code profile}), confirmed against a live deployment and
 * cross-referenced against observatory-ui's DataService (e.g. getQuestion6()/getQuestion7()).
 * Entirely different shape from OpenAireQueryBuilder's filter-tree DSL — this tool has no ad-hoc
 * filters/select, only a fixed, pre-registered query name per survey year and question code.
 */
@Component
public class SurveyStatsToolQueryBuilder {

    private final ObjectMapper objectMapper;
    private final String profile;

    public SurveyStatsToolQueryBuilder(ObjectMapper objectMapper, StatsToolProperties statsToolProperties) {
        this.objectMapper = objectMapper;
        this.profile = statsToolProperties.getSurveyProfile();
    }

    /** One series requesting a single survey question's per-country (or .sum aggregate) values for a stats-tool year. */
    public String surveyQuestionSeriesRequest(int statsYear, String questionCode) {
        ObjectNode query = objectMapper.createObjectNode();
        query.put("name", "eosc.sb." + questionCode);
        query.putObject("namedParameters").put("year", statsYear);
        query.put("profile", profile);

        ObjectNode series = objectMapper.createObjectNode();
        series.set("query", query);

        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("series").add(series);
        root.put("verbose", true);
        return root.toString();
    }
}
