package eu.openaire.observatory.indicator.dimension;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Shared search-text filtering, optional label sort, and limit — every {@link DimensionMemberProvider}
 * applies the same case-insensitive "label or code contains searchText" predicate to its own member
 * source, so only that source (and whether it's already sorted, e.g. a fixed hand-ordered vocabulary) varies.
 */
final class DimensionMemberSearch {

    private DimensionMemberSearch() {
    }

    static DimensionMembers filter(List<DimensionMember> members, String searchText, int limit, boolean sortByLabel) {
        String normalizedSearch = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);

        Stream<DimensionMember> matching = members.stream()
            .filter(member -> normalizedSearch.isEmpty()
                || member.label().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                || member.code().toLowerCase(Locale.ROOT).contains(normalizedSearch));
        if (sortByLabel) {
            matching = matching.sorted(Comparator.comparing(DimensionMember::label));
        }

        return new DimensionMembers(matching.limit(limit).toList());
    }
}
