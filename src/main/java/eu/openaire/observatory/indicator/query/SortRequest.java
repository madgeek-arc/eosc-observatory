package eu.openaire.observatory.indicator.query;

public record SortRequest(
    String field,
    SortDirection direction
) {
}
