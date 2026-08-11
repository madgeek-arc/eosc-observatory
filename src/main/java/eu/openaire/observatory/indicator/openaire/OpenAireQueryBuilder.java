package eu.openaire.observatory.indicator.openaire;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pure, network-free builder for OpenAIRE bibliometric stats-tool DSL requests (the "raw?json="
 * shape), translating the query-DSL already proven working in observatory-ui's
 * explore-queries.ts (OAPublicationVSClosed) into Java. Every sub-query scopes to European
 * publications for a given year; an optional access-mode filter narrows it further.
 */
@Component
public class OpenAireQueryBuilder {

    private static final String ENTITY = "result";
    private static final String PROFILE = "openaire_stats";
    private static final String RESULT_TYPE_PUBLICATION = "publication";
    private static final String COUNTRY_GROUP_FIELD = "result.organization.country";
    private static final String LIMIT = "30";
    private static final String UNLIMITED = "0";

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
        ObjectNode query = objectMapper.createObjectNode();
        query.putArray("parameters");
        query.putArray("select").addObject().put("field", ENTITY).put("aggregate", "count");

        ArrayNode filters = query.putArray("filters");
        commonFilters(filters, year, accessModes);
        if (countryCodes != null && !countryCodes.isEmpty()) {
            filters.add(countryFilter(countryCodes));
        }

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
        ObjectNode query = objectMapper.createObjectNode();
        query.putArray("parameters");
        ArrayNode select = query.putArray("select");
        select.addObject().put("field", ENTITY).put("aggregate", "count");
        ObjectNode countryGroup = select.addObject();
        countryGroup.put("field", COUNTRY_GROUP_FIELD);
        countryGroup.putNull("aggregate");

        ArrayNode filters = query.putArray("filters");
        commonFilters(filters, year, accessModes);

        query.put("entity", ENTITY);
        query.put("profile", PROFILE);
        query.put("limit", UNLIMITED);

        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.set("query", query);
        return wrapper;
    }

    /** The AND-filters every sub-query shares: optional access-mode, result type, Europe scope, year. */
    private void commonFilters(ArrayNode filters, int year, List<OpenAireAccessMode> accessModes) {
        if (accessModes != null && !accessModes.isEmpty()) {
            filters.add(accessModeFilter(accessModes));
        }
        filters.add(groupFilter("result.type", "=", RESULT_TYPE_PUBLICATION));
        filters.add(groupFilter("result.organization.country.continent", "=", "Europe"));
        filters.add(groupFilter("result.year", "=", String.valueOf(year)));
    }

    /** Wraps N sub-queries (each from publicationCountSubQuery) into one raw?json= request body, index-aligned with the response. */
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
