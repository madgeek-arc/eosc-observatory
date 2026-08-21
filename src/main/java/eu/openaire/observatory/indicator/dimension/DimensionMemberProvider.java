package eu.openaire.observatory.indicator.dimension;

import java.util.Set;
import java.util.stream.Collectors;

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
    DimensionMembers search(String indicatorCode, String searchText, int limit);

    /**
     * The full, real set of valid values for this dimension — the same enum, hardcoded vocabulary,
     * or live api/db call that backs {@link #search}. Default implementation just re-runs search()
     * with no search text and an unbounded limit; override only if a provider can compute this more
     * directly. {@code IndicatorQueryValidator} uses this to reject filter values that aren't a real
     * member of the dimension.
     */
    default Set<String> validCodes(String indicatorCode) {
        return search(indicatorCode, "", Integer.MAX_VALUE).members().stream()
            .map(DimensionMember::code)
            .collect(Collectors.toUnmodifiableSet());
    }
}
