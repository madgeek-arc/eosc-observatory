package eu.openaire.observatory.indicator.openaire;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.openaire.observatory.indicator.IndicatorCodes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Pure, network-free builder for OpenAIRE's stats-tool DSL requests (the "raw?json="
 * shape), translating the query-DSL already proven working in observatory-ui's
 * explore-queries.ts (OAPublicationVSClosed) into Java. Every sub-query scopes to European
 * results for a given year and entity type (publication or dataset); an optional access-mode
 * filter narrows it further.
 *
 * <p>Each sub-query's {@code op:"AND"} conditions are folded into a single {@code groupFilters}
 * block ({@link #foldAndBlocks}) to keep the generated {@code raw?json=} URL short; {@code OR}
 * blocks stay separate top-level blocks.
 */
@Component
public class OpenAireQueryBuilder {

    private static final String ENTITY = "result";
    private static final String PROFILE = "openaire_stats";
    /** Stats-tool profile behind observatory-ui's distributionOfOAPublications — confirmed live, distinct from PROFILE above. */
    private static final String EUROPE_MONITOR_PROFILE = "europe_monitor";
    public static final String RESULT_TYPE_PUBLICATION = "publication";
    public static final String RESULT_TYPE_DATASET = "dataset";
    public static final String RESULT_TYPE_SOFTWARE = "software";
    private static final String COUNTRY_GROUP_FIELD = "result.organization.country";
    /** The narrower classification vocabulary distributionOfOAPublications always applies (contains-match, lowercase) — confirmed live, distinct from the classification dimension's own "=" filter over the full capitalized vocabulary. */
    private static final List<String> OA_LICENSE_STATUS_CLASSIFICATIONS = List.of("article", "conference", "chapter", "book");
    private static final String LIMIT = "30";
    private static final String UNLIMITED = "0";

    /** OpenAIRE stats-tool field path for each groupable breakdown dimension — confirmed live: country/classification/fieldOfScience groupBy all return `[count, value]` rows. */
    private static final Map<String, String> GROUP_FIELD_BY_DIMENSION = Map.of(
        IndicatorCodes.COUNTRY_DIMENSION, COUNTRY_GROUP_FIELD,
        IndicatorCodes.CLASSIFICATION_DIMENSION, "result.classification",
        IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION, "result.result_fos.lvl1"
    );

    private final ObjectMapper objectMapper;

    public OpenAireQueryBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * One sub-query: European (or, if countryCodes is given, one or more specific countries')
     * publication count for the given year, optionally filtered by access mode. Zero/null
     * accessModes/countryCodes means no filter on that field at all; more than one value ORs them
     * together (e.g. "every non-open status", or "France or Germany").
     */
    public ObjectNode publicationCountSubQuery(int year, List<OpenAireAccessMode> accessModes, List<String> countryCodes) {
        return resultCountSubQuery(year, accessModes, DimensionFilters.ofCountry(countryCodes), RESULT_TYPE_PUBLICATION);
    }

    /** Same as {@link #publicationCountSubQuery}, generalized to any entity type (publication or dataset) and any combination of the 3 filterable dimensions. */
    public ObjectNode resultCountSubQuery(
            int year, List<OpenAireAccessMode> accessModes, DimensionFilters filters, String resultType) {
        ObjectNode query = objectMapper.createObjectNode();
        query.putArray("parameters");
        query.putArray("select").addObject().put("field", ENTITY).put("aggregate", "count");

        ArrayNode filterArray = objectMapper.createArrayNode();
        commonFilters(filterArray, year, accessModes, resultType);
        addDimensionFilters(filterArray, filters);
        query.set("filters", foldAndBlocks(filterArray));

        query.put("entity", ENTITY);
        query.put("profile", PROFILE);
        query.put("limit", LIMIT);

        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.set("query", query);
        return wrapper;
    }

    /**
     * Same scope as {@link #publicationCountSubQuery}, but groups by country instead of filtering to
     * one — a single sub-query returns one row per country (confirmed live against the real endpoint:
     * `[["33","AD"],["2143","AL"],...]`, `[count, countryCode]`), matching observatory-ui's
     * OAAndTotalPublicationsPerCountry query. Avoids issuing one sub-query per country.
     */
    public ObjectNode publicationCountByCountrySubQuery(int year, List<OpenAireAccessMode> accessModes) {
        return resultCountByGroupSubQuery(
            year, accessModes, RESULT_TYPE_PUBLICATION, IndicatorCodes.COUNTRY_DIMENSION, DimensionFilters.NONE);
    }

    /**
     * Same as {@link #publicationCountByCountrySubQuery}, generalized to any entity type, any
     * groupable breakdown dimension (country/classification/fieldOfScience — see
     * {@link #GROUP_FIELD_BY_DIMENSION}), plus any combination of the 3 filterable dimensions for
     * combining a breakdown with a narrower scope (confirmed live: country-filtered
     * fieldOfScience/classification groupBy, and classification-filtered country groupBy, both work).
     */
    public ObjectNode resultCountByGroupSubQuery(
            int year, List<OpenAireAccessMode> accessModes, String resultType,
            String groupDimensionCode, DimensionFilters filters) {
        ObjectNode query = objectMapper.createObjectNode();
        query.putArray("parameters");
        ArrayNode select = query.putArray("select");
        select.addObject().put("field", ENTITY).put("aggregate", "count");
        ObjectNode group = select.addObject();
        group.put("field", GROUP_FIELD_BY_DIMENSION.get(groupDimensionCode));
        group.putNull("aggregate");

        ArrayNode filterArray = objectMapper.createArrayNode();
        commonFilters(filterArray, year, accessModes, resultType);
        addDimensionFilters(filterArray, filters);
        query.set("filters", foldAndBlocks(filterArray));

        query.put("entity", ENTITY);
        query.put("profile", PROFILE);
        query.put("limit", UNLIMITED);

        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.set("query", query);
        return wrapper;
    }

    /** The AND-filters every sub-query shares: optional access-mode, result type, Europe scope, year. */
    private void commonFilters(ArrayNode filters, int year, List<OpenAireAccessMode> accessModes, String resultType) {
        if (accessModes != null && !accessModes.isEmpty()) {
            filters.add(accessModeFilter(accessModes));
        }
        filters.add(groupFilter("result.type", "=", resultType));
        filters.add(groupFilter("result.organization.country.continent", "=", "Europe"));
        filters.add(groupFilter("result.year", "=", String.valueOf(year)));
    }

    /** Adds one AND-block per non-empty dimension filter (each internally OR'd across its own values). */
    private void addDimensionFilters(ArrayNode filterArray, DimensionFilters filters) {
        if (!filters.countryCodes().isEmpty()) {
            filterArray.add(countryFilter(filters.countryCodes()));
        }
        if (!filters.classificationCodes().isEmpty()) {
            filterArray.add(classificationFilter(filters.classificationCodes()));
        }
        if (!filters.fieldOfScienceCodes().isEmpty()) {
            filterArray.add(fieldOfScienceFilter(filters.fieldOfScienceCodes()));
        }
        if (!filters.goldOaLiterals().isEmpty()) {
            filterArray.add(multiLeafGroupFilter("result.gold_oa", "=", filters.goldOaLiterals(), "AND"));
        }
        if (!filters.greenOaLiterals().isEmpty()) {
            filterArray.add(multiLeafGroupFilter("result.green_oa", "=", filters.greenOaLiterals(), "AND"));
        }
    }

    /**
     * Folds every {@code op:"AND"} block of a freshly built filter array into a single trailing AND
     * block. Semantically identical — the stats-tool AND-combines top-level filter blocks (confirmed
     * live in this repo: one combined {@code groupFilters} block and two separate top-level blocks
     * return identical results, see the {@code country} {@code NOT_IN} note in
     * {@code indicator/README.md}) — but it collapses the 4-8 single-leaf AND blocks every sub-query
     * carries (access mode, result type, Europe scope, year, dimension filters) into one, shrinking
     * the {@code raw?json=} URL by ~13% so wider year ranges stay under the upstream request-line
     * limit. OR blocks (multi access-mode, multi country, the classification-vocabulary
     * contains-match) are kept as separate top-level blocks.
     */
    private ArrayNode foldAndBlocks(ArrayNode builtFilters) {
        ArrayNode folded = objectMapper.createArrayNode();
        ObjectNode andBlock = objectMapper.createObjectNode();
        ArrayNode andLeaves = andBlock.putArray("groupFilters");
        for (JsonNode block : builtFilters) {
            if ("AND".equals(block.path("op").asText())) {
                block.path("groupFilters").forEach(andLeaves::add);
            } else {
                folded.add(block);
            }
        }
        if (!andLeaves.isEmpty()) {
            andBlock.put("op", "AND");
            folded.add(andBlock);
        }
        return folded;
    }

    /**
     * One sub-query for publications.oa_license_status_count: European (or, if countryCodes is
     * given, one or more specific countries') publication count for the given year and OA/license
     * status, grouped by classification (narrowed to the fixed article/conference/chapter/book
     * vocabulary), optionally also grouped by country — confirmed live against the europe_monitor
     * profile, including the two-select-group-field shape (returns {@code [count, classification,
     * country]} rows rather than {@code [count, classification]}), via a real {@code raw?json=} call.
     * The optional country filter reuses the same {@code result.organization.country.code} field/leaf
     * shape as every other sub-query in this class ({@link #countryFilter}).
     *
     * <p>{@code status} may be {@code null} — the denominator ("all four output types, any access
     * mode") sub-query the share views need: then the {@code result.access mode} and license-flag
     * filters are omitted and the query counts every publication in the classification vocabulary.
     */
    public ObjectNode oaLicenseStatusSubQuery(
            int year, OpenAireOaLicenseStatus status, List<String> countryCodes, boolean groupByCountry) {
        ObjectNode query = objectMapper.createObjectNode();
        query.putArray("parameters");
        ArrayNode select = query.putArray("select");
        select.addObject().put("field", ENTITY).put("aggregate", "count");
        ObjectNode classificationGroup = select.addObject();
        classificationGroup.put("field", GROUP_FIELD_BY_DIMENSION.get(IndicatorCodes.CLASSIFICATION_DIMENSION));
        classificationGroup.putNull("aggregate");
        if (groupByCountry) {
            ObjectNode countryGroup = select.addObject();
            countryGroup.put("field", COUNTRY_GROUP_FIELD);
            countryGroup.putNull("aggregate");
        }

        ArrayNode filterArray = objectMapper.createArrayNode();
        filterArray.add(classificationVocabularyFilter());
        filterArray.add(groupFilter("result.type", "=", RESULT_TYPE_PUBLICATION));
        if (status != null) {
            filterArray.add(groupFilter("result.access mode", "=", status.accessMode().openAireLiteral()));
            if (status.licenseField() != null) {
                filterArray.add(groupFilter(status.licenseField(), "=", "1"));
            }
        }
        filterArray.add(groupFilter("result.organization.country.continent", "=", "Europe"));
        filterArray.add(groupFilter("result.year", "=", String.valueOf(year)));
        if (countryCodes != null && !countryCodes.isEmpty()) {
            filterArray.add(countryFilter(countryCodes));
        }
        query.set("filters", foldAndBlocks(filterArray));

        query.put("entity", ENTITY);
        query.put("profile", EUROPE_MONITOR_PROFILE);
        query.put("limit", UNLIMITED);

        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.set("query", query);
        return wrapper;
    }

    /** The fixed article/conference/chapter/book contains-match OR block every oaLicenseStatus sub-query applies. */
    private ObjectNode classificationVocabularyFilter() {
        ObjectNode block = objectMapper.createObjectNode();
        ArrayNode groupFilters = block.putArray("groupFilters");
        for (String value : OA_LICENSE_STATUS_CLASSIFICATIONS) {
            ObjectNode leaf = groupFilters.addObject();
            leaf.put("field", GROUP_FIELD_BY_DIMENSION.get(IndicatorCodes.CLASSIFICATION_DIMENSION));
            leaf.put("type", "contains");
            leaf.putArray("values").add(value);
        }
        block.put("op", "OR");
        return block;
    }

    /** Wraps N sub-queries (each from publicationCountSubQuery or publicationCountByCountrySubQuery) into one raw?json= request body, index-aligned with the response. */
    public String seriesRequest(List<ObjectNode> subQueries) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("verbose", false);
        ArrayNode series = root.putArray("series");
        subQueries.forEach(series::add);
        return root.toString();
    }

    /** Single-literal AND-block, e.g. {"groupFilters":[{"field":...,"values":[value]}],"op":"AND"}. */
    private ObjectNode groupFilter(String field, String type, String value) {
        return multiLeafGroupFilter(field, type, List.of(value), "AND");
    }

    /**
     * One leaf per access mode, OR'd together when there's more than one — matches the "every
     * non-open status" shape confirmed in observatory-ui's coutry-pages.queries.ts "closed" series.
     */
    private ObjectNode accessModeFilter(List<OpenAireAccessMode> accessModes) {
        List<String> literals = accessModes.stream().map(OpenAireAccessMode::openAireLiteral).toList();
        String op = literals.size() > 1 ? "OR" : "AND";
        return multiLeafGroupFilter("result.access mode", "=", literals, op);
    }

    /** One leaf per country code, OR'd together when there's more than one (e.g. "France or Germany"). */
    private ObjectNode countryFilter(List<String> countryCodes) {
        String op = countryCodes.size() > 1 ? "OR" : "AND";
        return multiLeafGroupFilter("result.organization.country.code", "=", countryCodes, op);
    }

    /** One leaf per classification, OR'd together when there's more than one — same field as its GROUP breakdown (unlike country, no separate filter-only field). */
    private ObjectNode classificationFilter(List<String> classifications) {
        String op = classifications.size() > 1 ? "OR" : "AND";
        return multiLeafGroupFilter(GROUP_FIELD_BY_DIMENSION.get(IndicatorCodes.CLASSIFICATION_DIMENSION), "=", classifications, op);
    }

    /** One leaf per field of science, OR'd together when there's more than one — same field as its GROUP breakdown. */
    private ObjectNode fieldOfScienceFilter(List<String> fieldsOfScience) {
        String op = fieldsOfScience.size() > 1 ? "OR" : "AND";
        return multiLeafGroupFilter(GROUP_FIELD_BY_DIMENSION.get(IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION), "=", fieldsOfScience, op);
    }

    private ObjectNode multiLeafGroupFilter(String field, String type, List<String> values, String op) {
        ObjectNode block = objectMapper.createObjectNode();
        ArrayNode groupFilters = block.putArray("groupFilters");
        for (String value : values) {
            ObjectNode leaf = groupFilters.addObject();
            leaf.put("field", field);
            leaf.put("type", type);
            leaf.putArray("values").add(value);
        }
        block.put("op", op);
        return block;
    }
}
