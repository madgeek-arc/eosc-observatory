package eu.openaire.observatory.indicator.model;

/** Join between an indicator and a dimension it can be filtered/grouped by. */
public record IndicatorDimensionRef(
    String dimensionCode,
    boolean required
) {
}
