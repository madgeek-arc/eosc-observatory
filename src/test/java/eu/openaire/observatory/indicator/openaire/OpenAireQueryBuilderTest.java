package eu.openaire.observatory.indicator.openaire;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.openaire.observatory.indicator.IndicatorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure, network-free: asserts the DSL shape built for OpenAIRE's stats-tool "raw?json=" endpoint
 * matches the field names/values confirmed against observatory-ui's OAPublicationVSClosed query
 * (access-mode filtering) and coutry-pages.queries.ts (the "closed" OR-block and the
 * result.organization.country.code country filter).
 *
 * <p>Every {@code op:"AND"} condition a sub-query carries is folded into one shared
 * {@code groupFilters} block ({@link OpenAireQueryBuilder#foldAndBlocks}); {@code OR} blocks stay
 * separate. The helpers below therefore look leaves up <em>across all blocks</em>.
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
        // Single access mode is an AND condition, so it folds in with type/continent/year.
        assertThat(filters).hasSize(1);
        assertThat(opForField(filters, "result.access mode")).isEqualTo("AND");
        assertThat(leafValues(filters, "result.access mode")).containsExactly("Closed Access");
        assertThat(leafValues(filters, "result.type")).containsExactly("publication");
        assertThat(leafValues(filters, "result.organization.country.continent")).containsExactly("Europe");
        assertThat(leafValues(filters, "result.year")).containsExactly("2023");
    }

    @Test
    void publicationCountSubQueryWithMultipleAccessModesOrsThemTogether() {
        ObjectNode subQuery = builder.publicationCountSubQuery(
            2023, List.of(OpenAireAccessMode.CLOSED, OpenAireAccessMode.EMBARGOED, OpenAireAccessMode.RESTRICTED), null
        );

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(opForField(filters, "result.access mode")).isEqualTo("OR");
        assertThat(leafValues(filters, "result.access mode"))
            .containsExactlyInAnyOrder("Closed Access", "Embargo", "Restricted");
    }

    @Test
    void publicationCountSubQueryWithoutAccessModeOmitsThatFilter() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(), null);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(filters).hasSize(1);
        assertThat(fieldsPresent(filters)).doesNotContain("result.access mode");
    }

    @Test
    void publicationCountSubQueryWithCountryCodeAddsCountryFilter() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(), List.of("GR"));

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        // Single country is an AND condition too — one folded block.
        assertThat(filters).hasSize(1);
        assertThat(opForField(filters, "result.organization.country.code")).isEqualTo("AND");
        assertThat(leafValues(filters, "result.organization.country.code")).containsExactly("GR");
        // Country given still keeps the continent scope — narrowing, not replacing it.
        assertThat(leafValues(filters, "result.organization.country.continent")).containsExactly("Europe");
    }

    @Test
    void publicationCountSubQueryWithMultipleCountryCodesPutsThemInOneLeaf() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(), List.of("GR", "DE"));

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(opForField(filters, "result.organization.country.code")).isEqualTo("AND");
        assertThat(allLeafValues(filters, "result.organization.country.code")).containsExactly("GR", "DE");
        assertThat(filters).noneMatch(block -> "OR".equals(block.path("op").asText()));
    }

    @Test
    void publicationCountSubQueryWithoutCountryCodeOmitsCountryFilter() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(), null);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(fieldsPresent(filters)).doesNotContain("result.organization.country.code");
    }

    @Test
    void publicationCountSubQueryWithEmptyCountryListOmitsCountryFilter() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(), List.of());

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(fieldsPresent(filters)).doesNotContain("result.organization.country.code");
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
        assertThat(fieldsPresent(filters)).doesNotContain("result.organization.country.code");
        assertThat(leafValues(filters, "result.access mode")).containsExactly("Open Access");
        assertThat(leafValues(filters, "result.type")).containsExactly("publication");
        assertThat(leafValues(filters, "result.organization.country.continent")).containsExactly("Europe");
        assertThat(leafValues(filters, "result.year")).containsExactly("2023");
    }

    @Test
    void publicationCountByCountrySubQueryWithoutAccessModeOmitsThatFilter() {
        ObjectNode subQuery = builder.publicationCountByCountrySubQuery(2023, List.of());

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(filters).hasSize(1);
        assertThat(fieldsPresent(filters)).doesNotContain("result.access mode");
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

    @Test
    void publicationCountSubQueryFoldsEveryAndConditionIntoOneBlock() {
        ObjectNode subQuery = builder.publicationCountSubQuery(2023, List.of(OpenAireAccessMode.OA), null);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(filters).hasSize(1);
        assertThat(filters.get(0).path("op").asText()).isEqualTo("AND");
        assertThat(fieldsInBlock(filters.get(0))).containsExactlyInAnyOrder(
            "result.access mode", "result.type", "result.organization.country.continent", "result.year");
    }

    @Test
    void publicationCountSubQueryKeepsAnOrBlockAlongsideTheFoldedAndBlock() {
        ObjectNode subQuery = builder.publicationCountSubQuery(
            2023, List.of(OpenAireAccessMode.CLOSED, OpenAireAccessMode.EMBARGOED), null);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(filters).hasSize(2);

        JsonNode orBlock = blockContaining(filters, "result.access mode");
        assertThat(orBlock.path("op").asText()).isEqualTo("OR");
        assertThat(fieldsInBlock(orBlock)).containsOnly("result.access mode");

        JsonNode andBlock = blockContaining(filters, "result.year");
        assertThat(andBlock.path("op").asText()).isEqualTo("AND");
        assertThat(fieldsInBlock(andBlock)).containsExactlyInAnyOrder(
            "result.type", "result.organization.country.continent", "result.year");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"dataset", "software"})
    void resultCountSubQueryFiltersByTheGivenResultType(String resultType) {
        ObjectNode subQuery = builder.resultCountSubQuery(2023, List.of(), DimensionFilters.NONE, resultType);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(leafValues(filters, "result.type")).containsExactly(resultType);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"dataset", "software"})
    void resultCountByGroupSubQueryFiltersByTheGivenResultType(String resultType) {
        ObjectNode subQuery = builder.resultCountByGroupSubQuery(
            2023, List.of(), resultType, IndicatorCodes.CLASSIFICATION_DIMENSION, DimensionFilters.NONE);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(leafValues(filters, "result.type")).containsExactly(resultType);
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
        assertThat(leafValues(filters, "result.organization.country.code")).containsExactly("GR");
        // The Europe continent scope still applies alongside the narrower country filter.
        assertThat(leafValues(filters, "result.organization.country.continent")).containsExactly("Europe");
    }

    @Test
    void resultCountByGroupSubQueryWithoutCountryCodesOmitsCountryFilter() {
        ObjectNode subQuery = builder.resultCountByGroupSubQuery(
            2023, List.of(), OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION, IndicatorCodes.CLASSIFICATION_DIMENSION, DimensionFilters.NONE);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(fieldsPresent(filters)).doesNotContain("result.organization.country.code");
    }

    @Test
    void resultCountSubQueryWithClassificationFilterUsesTheSameFieldAsItsGroupBreakdown() {
        var filters = new DimensionFilters(List.of(), List.of("Article"), List.of(), List.of(), List.of());
        ObjectNode subQuery = builder.resultCountSubQuery(2023, List.of(), filters, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        List<JsonNode> resultFilters = toList(subQuery.path("query").path("filters"));
        assertThat(opForField(resultFilters, "result.classification")).isEqualTo("AND");
        assertThat(leafValues(resultFilters, "result.classification")).containsExactly("Article");
    }

    @Test
    void resultCountSubQueryWithMultipleClassificationsOrsThemTogether() {
        var filters = new DimensionFilters(List.of(), List.of("Article", "Book"), List.of(), List.of(), List.of());
        ObjectNode subQuery = builder.resultCountSubQuery(2023, List.of(), filters, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        List<JsonNode> resultFilters = toList(subQuery.path("query").path("filters"));
        assertThat(opForField(resultFilters, "result.classification")).isEqualTo("OR");
        assertThat(leafValues(resultFilters, "result.classification")).containsExactlyInAnyOrder("Article", "Book");
    }

    @Test
    void resultCountSubQueryWithFieldOfScienceFilterUsesTheSameFieldAsItsGroupBreakdown() {
        var filters = new DimensionFilters(List.of(), List.of(), List.of("01 natural sciences"), List.of(), List.of());
        ObjectNode subQuery = builder.resultCountSubQuery(2023, List.of(), filters, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        List<JsonNode> resultFilters = toList(subQuery.path("query").path("filters"));
        assertThat(leafValues(resultFilters, "result.result_fos.lvl1")).containsExactly("01 natural sciences");
    }

    @Test
    void resultCountByGroupSubQueryCombinesAClassificationFilterWithACountryGroupBreakdown() {
        var filters = new DimensionFilters(List.of(), List.of("Article"), List.of(), List.of(), List.of());
        ObjectNode subQuery = builder.resultCountByGroupSubQuery(
            2023, List.of(), OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION, IndicatorCodes.COUNTRY_DIMENSION, filters);

        JsonNode query = subQuery.path("query");
        assertThat(query.path("select").get(1).path("field").asText()).isEqualTo("result.organization.country");

        List<JsonNode> resultFilters = toList(query.path("filters"));
        assertThat(leafValues(resultFilters, "result.classification")).containsExactly("Article");
    }

    @Test
    void resultCountSubQueryWithGoldOaFilterAddsGoldOaField() {
        var filters = new DimensionFilters(List.of(), List.of(), List.of(), List.of("1"), List.of());
        ObjectNode subQuery = builder.resultCountSubQuery(2022, List.of(), filters, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        List<JsonNode> resultFilters = toList(subQuery.path("query").path("filters"));
        assertThat(leafValues(resultFilters, "result.gold_oa")).containsExactly("1");
        assertThat(fieldsPresent(resultFilters)).doesNotContain("result.green_oa");
    }

    @Test
    void resultCountSubQueryWithGreenOaFilterAddsGreenOaField() {
        var filters = new DimensionFilters(List.of(), List.of(), List.of(), List.of(), List.of("1"));
        ObjectNode subQuery = builder.resultCountSubQuery(2022, List.of(), filters, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        List<JsonNode> resultFilters = toList(subQuery.path("query").path("filters"));
        assertThat(leafValues(resultFilters, "result.green_oa")).containsExactly("1");
        assertThat(fieldsPresent(resultFilters)).doesNotContain("result.gold_oa");
    }

    @Test
    void resultCountSubQueryFoldsGoldAndGreenOaIntoTheSharedAndBlock() {
        var filters = new DimensionFilters(List.of(), List.of(), List.of(), List.of("1"), List.of("1"));
        ObjectNode subQuery = builder.resultCountSubQuery(2022, List.of(), filters, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        List<JsonNode> resultFilters = toList(subQuery.path("query").path("filters"));
        // Both gold_oa and green_oa are AND conditions — they land in the one shared AND block.
        assertThat(resultFilters).hasSize(1);
        assertThat(resultFilters.get(0).path("op").asText()).isEqualTo("AND");
        assertThat(opForField(resultFilters, "result.gold_oa")).isEqualTo("AND");
        assertThat(opForField(resultFilters, "result.green_oa")).isEqualTo("AND");
        assertThat(leafValues(resultFilters, "result.gold_oa")).containsExactly("1");
        assertThat(leafValues(resultFilters, "result.green_oa")).containsExactly("1");
    }

    @Test
    void resultCountSubQueryWithoutGoldOrGreenOaOmitsBothFilters() {
        ObjectNode subQuery = builder.resultCountSubQuery(2022, List.of(), DimensionFilters.NONE, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        List<JsonNode> resultFilters = toList(subQuery.path("query").path("filters"));
        assertThat(fieldsPresent(resultFilters)).doesNotContain("result.gold_oa", "result.green_oa");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(OpenAireOaLicenseStatus.class)
    void oaLicenseStatusSubQueryUsesTheEuropeMonitorProfile(OpenAireOaLicenseStatus status) {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, status, List.of(), false);

        assertThat(subQuery.path("query").path("profile").asText()).isEqualTo("europe_monitor");
        assertThat(subQuery.path("query").path("limit").asText()).isEqualTo("0");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(OpenAireOaLicenseStatus.class)
    void oaLicenseStatusSubQueryFiltersByTheStatusAccessMode(OpenAireOaLicenseStatus status) {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, status, List.of(), false);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(leafValues(filters, "result.access mode")).containsExactly(status.accessMode().openAireLiteral());
        assertThat(leafValues(filters, "result.type")).containsExactly("publication");
        assertThat(leafValues(filters, "result.organization.country.continent")).containsExactly("Europe");
        assertThat(leafValues(filters, "result.year")).containsExactly("2022");
    }

    @Test
    void oaLicenseStatusSubQueryForWithLicenseAddsTheLicenseFlagFilter() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, OpenAireOaLicenseStatus.OA_WITH_LICENSE, List.of(), false);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(leafValues(filters, "result.indi_result_oa_with_license.oa_with_license")).containsExactly("1");
    }

    @Test
    void oaLicenseStatusSubQueryForWithoutLicenseAddsTheOtherLicenseFlagFilter() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, OpenAireOaLicenseStatus.OA_WITHOUT_LICENSE, List.of(), false);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(leafValues(filters, "result.indi_result_oa_without_license.oa_without_license")).containsExactly("1");
    }

    @Test
    void oaLicenseStatusSubQueryForClosedOmitsAnyLicenseFlagFilter() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, OpenAireOaLicenseStatus.CLOSED, List.of(), false);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(fieldsPresent(filters))
            .doesNotContain("result.indi_result_oa_with_license.oa_with_license")
            .doesNotContain("result.indi_result_oa_without_license.oa_without_license");
    }

    @Test
    void oaLicenseStatusSubQueryWithNullStatusOmitsTheAccessModeAndLicenceLeaves() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, null, List.of(), false);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(fieldsPresent(filters))
            .doesNotContain("result.access mode",
                "result.indi_result_oa_with_license.oa_with_license",
                "result.indi_result_oa_without_license.oa_without_license");
        // still the classification vocabulary + entity type + Europe + year scope
        assertThat(leafValues(filters, "result.classification"))
            .containsExactlyInAnyOrder("article", "conference", "chapter", "book");
        assertThat(leafValues(filters, "result.year")).containsExactly("2022");
        assertThat(leafValues(filters, "result.organization.country.continent")).containsExactly("Europe");
    }

    @Test
    void oaLicenseStatusSubQueryNarrowsClassificationToTheFixedVocabulary() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, OpenAireOaLicenseStatus.CLOSED, List.of(), false);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(opForField(filters, "result.classification")).isEqualTo("OR");
        assertThat(leafValues(filters, "result.classification"))
            .containsExactlyInAnyOrder("article", "conference", "chapter", "book");
        JsonNode classificationBlock = blockContaining(filters, "result.classification");
        assertThat(toList(classificationBlock.path("groupFilters")).get(0).path("type").asText()).isEqualTo("contains");
    }

    @Test
    void oaLicenseStatusSubQueryKeepsTheClassificationVocabularyAsItsOwnOrBlock() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, OpenAireOaLicenseStatus.CLOSED, List.of(), false);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        // one OR block (the classification vocabulary) + one folded AND block (everything else).
        assertThat(filters).hasSize(2);
        assertThat(blockContaining(filters, "result.classification").path("op").asText()).isEqualTo("OR");
        JsonNode andBlock = blockContaining(filters, "result.year");
        assertThat(andBlock.path("op").asText()).isEqualTo("AND");
        assertThat(fieldsInBlock(andBlock)).containsExactlyInAnyOrder(
            "result.type", "result.access mode", "result.organization.country.continent", "result.year");
    }

    @Test
    void oaLicenseStatusSubQueryGroupsByClassificationField() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, OpenAireOaLicenseStatus.CLOSED, List.of(), false);

        List<JsonNode> select = toList(subQuery.path("query").path("select"));
        assertThat(select.get(1).path("field").asText()).isEqualTo("result.classification");
        assertThat(select.get(1).path("aggregate").isNull()).isTrue();
    }

    @Test
    void oaLicenseStatusSubQueryWithCountryCodesAddsCountryFilter() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, OpenAireOaLicenseStatus.CLOSED, List.of("GR", "DE"), false);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(opForField(filters, "result.organization.country.code")).isEqualTo("AND");
        assertThat(allLeafValues(filters, "result.organization.country.code")).containsExactly("GR", "DE");
        // Country given still keeps the continent scope — narrowing, not replacing it.
        assertThat(leafValues(filters, "result.organization.country.continent")).containsExactly("Europe");
    }

    @Test
    void oaLicenseStatusSubQueryWithoutCountryCodesOmitsCountryFilter() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, OpenAireOaLicenseStatus.CLOSED, List.of(), false);

        List<JsonNode> filters = toList(subQuery.path("query").path("filters"));
        assertThat(fieldsPresent(filters)).doesNotContain("result.organization.country.code");
    }

    /**
     * Confirmed live against the real europe_monitor endpoint: a second simultaneous select-group
     * field (classification + country) is accepted and returns {@code [count, classification,
     * country]} rows, e.g. {@code {"data":[[["346231","Article"],...` becomes
     * {@code [["2","Article","AD"],["177","Article","AL"],...]}.
     */
    @Test
    void oaLicenseStatusSubQueryWithGroupByCountryAddsASecondSelectGroupField() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, OpenAireOaLicenseStatus.CLOSED, List.of(), true);

        List<JsonNode> select = toList(subQuery.path("query").path("select"));
        assertThat(select).hasSize(3);
        assertThat(select.get(1).path("field").asText()).isEqualTo("result.classification");
        assertThat(select.get(2).path("field").asText()).isEqualTo("result.organization.country");
        assertThat(select.get(2).path("aggregate").isNull()).isTrue();
    }

    @Test
    void oaLicenseStatusSubQueryWithoutGroupByCountryOmitsTheSecondSelectGroupField() {
        ObjectNode subQuery = builder.oaLicenseStatusSubQuery(2022, OpenAireOaLicenseStatus.CLOSED, List.of(), false);

        List<JsonNode> select = toList(subQuery.path("query").path("select"));
        assertThat(select).hasSize(2);
    }

    private List<JsonNode> toList(JsonNode arrayNode) {
        return StreamSupport.stream(arrayNode.spliterator(), false).toList();
    }

    /** Every leaf {@code field} across every filter block. */
    private List<String> fieldsPresent(List<JsonNode> filters) {
        return filters.stream()
            .flatMap(block -> toList(block.path("groupFilters")).stream())
            .map(leaf -> leaf.path("field").asText())
            .toList();
    }

    /** The {@code field}s of the leaves within one block. */
    private List<String> fieldsInBlock(JsonNode block) {
        return toList(block.path("groupFilters")).stream()
            .map(leaf -> leaf.path("field").asText())
            .toList();
    }

    /** The one block whose leaves include the given field. */
    private JsonNode blockContaining(List<JsonNode> filters, String field) {
        return filters.stream()
            .filter(block -> fieldsInBlock(block).contains(field))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no filter block carries a leaf for " + field));
    }

    /** {@code op} of the block carrying the given field. */
    private String opForField(List<JsonNode> filters, String field) {
        return blockContaining(filters, field).path("op").asText();
    }

    /** All leaf values (one per literal) for the given field, in whichever block they sit. */
    private List<String> leafValues(List<JsonNode> filters, String field) {
        return filters.stream()
            .flatMap(block -> toList(block.path("groupFilters")).stream())
            .filter(leaf -> field.equals(leaf.path("field").asText()))
            .map(leaf -> leaf.path("values").get(0).asText())
            .toList();
    }

    /** Every value of every leaf for the given field, across all blocks. */
    private List<String> allLeafValues(List<JsonNode> filters, String field) {
        return filters.stream()
            .flatMap(block -> toList(block.path("groupFilters")).stream())
            .filter(leaf -> field.equals(leaf.path("field").asText()))
            .flatMap(leaf -> toList(leaf.path("values")).stream())
            .map(JsonNode::asText)
            .toList();
    }
}
