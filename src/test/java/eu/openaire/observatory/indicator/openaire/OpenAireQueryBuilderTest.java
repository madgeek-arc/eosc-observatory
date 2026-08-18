package eu.openaire.observatory.indicator.openaire;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.openaire.observatory.indicator.IndicatorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure, network-free: asserts the DSL shape built for OpenAIRE's stats-tool "raw?json=" endpoint
 * matches the field names/values confirmed against observatory-ui's OAPublicationVSClosed query
 * (access-mode filtering) and coutry-pages.queries.ts (the "closed" OR-block and the
 * result.organization.country.code country filter).
 */
class OpenAireQueryBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAireQueryBuilder builder = new OpenAireQueryBuilder(objectMapper);

    @Test
    void publicationCountSubQueryWithSingleAccessModeFilter() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(OpenAireAccessMode.CLOSED), null);

        JsonNode query = subQuery.path("query");
        assertThat(query.path("entity").asText()).isEqualTo("result");
        assertThat(query.path("profile").asText()).isEqualTo("openaire_stats");
        assertThat(query.path("select").get(0).path("field").asText()).isEqualTo("result");
        assertThat(query.path("select").get(0).path("aggregate").asText()).isEqualTo("count");

        List<JsonNode> filters = toList(query.path("filters"));
        assertThat(filters).hasSize(4);
        assertThat(opOf(filters, "result.access mode")).isEqualTo("AND");
        assertThat(fieldValues(filters, "result.access mode")).containsExactly("Closed Access");
        assertThat(fieldValues(filters, "result.type")).containsExactly("publication");
        assertThat(fieldValues(filters, "result.organization.country.continent")).containsExactly("Europe");
        assertThat(fieldValues(filters, "result.year")).containsExactly("2023");
    }

    @Test
    void publicationCountSubQueryWithMultipleAccessModesOrsThemTogether() {
        ObjectNode subQuery = builder.publicationCountSubQuery(
            2023, List.of(OpenAireAccessMode.CLOSED, OpenAireAccessMode.EMBARGOED, OpenAireAccessMode.RESTRICTED), null
        );

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(opOf(filters, "result.access mode")).isEqualTo("OR");
        assertThat(fieldValues(filters, "result.access mode"))
            .containsExactlyInAnyOrder("Closed Access", "Embargo", "Restricted");
    }

    @Test
    void publicationCountSubQueryWithoutAccessModeOmitsThatFilter() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(), null);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(filters).hasSize(3);
        assertThat(filters.stream().map(this::fieldOf)).doesNotContain("result.access mode");
    }

    @Test
    void publicationCountSubQueryWithCountryCodeAddsCountryFilter() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(), List.of("GR"));

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(filters).hasSize(4);
        assertThat(opOf(filters, "result.organization.country.code")).isEqualTo("AND");
        assertThat(fieldValues(filters, "result.organization.country.code")).containsExactly("GR");
        // Country given still keeps the continent scope — narrowing, not replacing it.
        assertThat(fieldValues(filters, "result.organization.country.continent")).containsExactly("Europe");
    }

    @Test
    void publicationCountSubQueryWithMultipleCountryCodesOrsThemTogether() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(), List.of("GR", "DE"));

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(opOf(filters, "result.organization.country.code")).isEqualTo("OR");
        assertThat(fieldValues(filters, "result.organization.country.code")).containsExactlyInAnyOrder("GR", "DE");
    }

    @Test
    void publicationCountSubQueryWithoutCountryCodeOmitsCountryFilter() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(), null);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(filters.stream().map(this::fieldOf)).doesNotContain("result.organization.country.code");
    }

    @Test
    void publicationCountSubQueryWithEmptyCountryListOmitsCountryFilter() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(), List.of());

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(filters.stream().map(this::fieldOf)).doesNotContain("result.organization.country.code");
    }

    @Test
    void publicationCountByCountrySubQueryGroupsByCountryInsteadOfFiltering() {
        ObjectNode subQuery = builder.publicationCountByCountrySubQuery(2023, List.of(OpenAireAccessMode.OA));

        JsonNode query = subQuery.path("query");
        assertThat(query.path("limit").asText()).isEqualTo("0");

        List<JsonNode> select = toList(query.path("select"));
        assertThat(select).hasSize(2);
        assertThat(select.get(0).path("field").asText()).isEqualTo("result");
        assertThat(select.get(0).path("aggregate").asText()).isEqualTo("count");
        assertThat(select.get(1).path("field").asText()).isEqualTo("result.organization.country");
        assertThat(select.get(1).path("aggregate").isNull()).isTrue();

        List<JsonNode> filters = toList(query.path("filters"));
        assertThat(filters.stream().map(this::fieldOf)).doesNotContain("result.organization.country.code");
        assertThat(fieldValues(filters, "result.access mode")).containsExactly("Open Access");
        assertThat(fieldValues(filters, "result.type")).containsExactly("publication");
        assertThat(fieldValues(filters, "result.organization.country.continent")).containsExactly("Europe");
        assertThat(fieldValues(filters, "result.year")).containsExactly("2023");
    }

    @Test
    void publicationCountByCountrySubQueryWithoutAccessModeOmitsThatFilter() {
        ObjectNode subQuery = builder.publicationCountByCountrySubQuery(2023, List.of());

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(filters).hasSize(3);
        assertThat(filters.stream().map(this::fieldOf)).doesNotContain("result.access mode");
    }

    @Test
    void seriesRequestWrapsSubQueriesInOrder() throws JsonProcessingException {
        ObjectNode first = builder.publicationCountSubQuery(2021, List.of(OpenAireAccessMode.OA), null);
        ObjectNode second = builder.publicationCountSubQuery(2021, List.of(), null);

        String json = builder.seriesRequest(List.of(first, second));

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("verbose").asBoolean()).isFalse();
        assertThat(root.path("series").size()).isEqualTo(2);
        assertThat(root.path("series").get(0)).isEqualTo(first);
        assertThat(root.path("series").get(1)).isEqualTo(second);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"dataset", "software"})
    void resultCountSubQueryFiltersByTheGivenResultType(String resultType) {
        ObjectNode subQuery = builder.resultCountSubQuery(2023, List.of(), DimensionFilters.NONE, resultType);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(fieldValues(filters, "result.type")).containsExactly(resultType);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"dataset", "software"})
    void resultCountByGroupSubQueryFiltersByTheGivenResultType(String resultType) {
        ObjectNode subQuery = builder.resultCountByGroupSubQuery(
            2023, List.of(), resultType, IndicatorCodes.CLASSIFICATION_DIMENSION, DimensionFilters.NONE);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(fieldValues(filters, "result.type")).containsExactly(resultType);
    }

    @Test
    void resultCountByGroupSubQueryGroupsByClassificationField() {
        ObjectNode subQuery = builder.resultCountByGroupSubQuery(
            2023, List.of(), OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION, IndicatorCodes.CLASSIFICATION_DIMENSION, DimensionFilters.NONE);

        JsonNode query = subQuery.path("query");
        List<JsonNode> select = toList(query.path("select"));
        assertThat(select.get(1).path("field").asText()).isEqualTo("result.classification");
        assertThat(select.get(1).path("aggregate").isNull()).isTrue();
    }

    @Test
    void resultCountByGroupSubQueryGroupsByFieldOfScienceField() {
        ObjectNode subQuery = builder.resultCountByGroupSubQuery(
            2023, List.of(), OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION, IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION, DimensionFilters.NONE);

        List<JsonNode> select = toList(subQuery.path("query").path("select"));
        assertThat(select.get(1).path("field").asText()).isEqualTo("result.result_fos.lvl1");
    }

    @Test
    void resultCountByGroupSubQueryCombinesANonCountryBreakdownWithACountryFilter() {
        ObjectNode subQuery = builder.resultCountByGroupSubQuery(
            2023, List.of(), OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION,
            IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION, DimensionFilters.ofCountry(List.of("GR")));

        JsonNode query = subQuery.path("query");
        assertThat(query.path("select").get(1).path("field").asText()).isEqualTo("result.result_fos.lvl1");

        List<JsonNode> filters = toList(query.path("filters"));
        assertThat(fieldValues(filters, "result.organization.country.code")).containsExactly("GR");
        // The Europe continent scope still applies alongside the narrower country filter.
        assertThat(fieldValues(filters, "result.organization.country.continent")).containsExactly("Europe");
    }

    @Test
    void resultCountByGroupSubQueryWithoutCountryCodesOmitsCountryFilter() {
        ObjectNode subQuery = builder.resultCountByGroupSubQuery(
            2023, List.of(), OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION, IndicatorCodes.CLASSIFICATION_DIMENSION, DimensionFilters.NONE);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(filters.stream().map(this::fieldOf)).doesNotContain("result.organization.country.code");
    }

    @Test
    void resultCountSubQueryWithClassificationFilterUsesTheSameFieldAsItsGroupBreakdown() {
        var filters = new DimensionFilters(List.of(), List.of("Article"), List.of());
        ObjectNode subQuery = builder.resultCountSubQuery(2023, List.of(), filters, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        List<JsonNode> resultFilters = toList(subQuery.path("query").path("filters"));
        assertThat(opOf(resultFilters, "result.classification")).isEqualTo("AND");
        assertThat(fieldValues(resultFilters, "result.classification")).containsExactly("Article");
    }

    @Test
    void resultCountSubQueryWithMultipleClassificationsOrsThemTogether() {
        var filters = new DimensionFilters(List.of(), List.of("Article", "Book"), List.of());
        ObjectNode subQuery = builder.resultCountSubQuery(2023, List.of(), filters, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        List<JsonNode> resultFilters = toList(subQuery.path("query").path("filters"));
        assertThat(opOf(resultFilters, "result.classification")).isEqualTo("OR");
        assertThat(fieldValues(resultFilters, "result.classification")).containsExactlyInAnyOrder("Article", "Book");
    }

    @Test
    void resultCountSubQueryWithFieldOfScienceFilterUsesTheSameFieldAsItsGroupBreakdown() {
        var filters = new DimensionFilters(List.of(), List.of(), List.of("01 natural sciences"));
        ObjectNode subQuery = builder.resultCountSubQuery(2023, List.of(), filters, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        List<JsonNode> resultFilters = toList(subQuery.path("query").path("filters"));
        assertThat(fieldValues(resultFilters, "result.result_fos.lvl1")).containsExactly("01 natural sciences");
    }

    @Test
    void resultCountByGroupSubQueryCombinesAClassificationFilterWithACountryGroupBreakdown() {
        var filters = new DimensionFilters(List.of(), List.of("Article"), List.of());
        ObjectNode subQuery = builder.resultCountByGroupSubQuery(
            2023, List.of(), OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION, IndicatorCodes.COUNTRY_DIMENSION, filters);

        JsonNode query = subQuery.path("query");
        assertThat(query.path("select").get(1).path("field").asText()).isEqualTo("result.organization.country");

        List<JsonNode> resultFilters = toList(query.path("filters"));
        assertThat(fieldValues(resultFilters, "result.classification")).containsExactly("Article");
    }

    private List<JsonNode> toList(JsonNode arrayNode) {
        return StreamSupport.stream(arrayNode.spliterator(), false).toList();
    }

    private String fieldOf(JsonNode groupFilterBlock) {
        return groupFilterBlock.path("groupFilters").get(0).path("field").asText();
    }

    private JsonNode blockFor(List<JsonNode> filters, String field) {
        return filters.stream().filter(f -> field.equals(fieldOf(f))).findFirst().orElseThrow();
    }

    private String opOf(List<JsonNode> filters, String field) {
        return blockFor(filters, field).path("op").asText();
    }

    /** All leaf values (one leaf per literal) within the block for the given field. */
    private List<String> fieldValues(List<JsonNode> filters, String field) {
        return toList(blockFor(filters, field).path("groupFilters")).stream()
            .map(leaf -> leaf.path("values").get(0).asText())
            .toList();
    }
}
