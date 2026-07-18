package eu.openaire.observatory.indicator.dimension;

/**
 * Looks up the valid member values of a filterable/groupable dimension (e.g. "which countries can
 * I filter/group 'country' by") — the seam a dashboard builder uses to populate a picker. Local and
 * externally-delegated implementations are both possible, mirroring the local/external split on
 * IndicatorQueryHandler.
 */
public interface DimensionMemberProvider {

    /** The single dimension code this provider serves, used by {@link DimensionMemberProviderRegistry} to dispatch. */
    String dimensionCode();

    /**
     * @param indicatorCode optional; narrows members to what's valid for a specific indicator when
     *                       an implementation supports per-indicator scoping. Unused by
     *                       single-dimension implementations that have nothing to narrow.
     */
    DimensionMemberPage search(String dimensionCode, String indicatorCode, String searchText, int limit);
}
