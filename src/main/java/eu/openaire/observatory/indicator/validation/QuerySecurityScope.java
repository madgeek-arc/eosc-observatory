package eu.openaire.observatory.indicator.validation;

import java.util.Set;

/**
 * Server-derived row-level scope for a compiled query. Must come from trusted server context
 * (see {@link IndicatorAuthorizationService}) — never from the client request. Null collections
 * mean unrestricted for that dimension; handlers are expected to apply this scope in addition to
 * the plan's own filters.
 */
public record QuerySecurityScope(
    String tenantId,
    Set<String> allowedOrganizationIds,
    Set<String> allowedCountryCodes
) {
    public QuerySecurityScope {
        allowedOrganizationIds = allowedOrganizationIds != null ? Set.copyOf(allowedOrganizationIds) : null;
        allowedCountryCodes = allowedCountryCodes != null ? Set.copyOf(allowedCountryCodes) : null;
    }

    public static QuerySecurityScope unrestricted() {
        return new QuerySecurityScope(null, null, null);
    }
}
