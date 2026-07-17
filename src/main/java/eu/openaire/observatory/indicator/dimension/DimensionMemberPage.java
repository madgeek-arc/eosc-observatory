package eu.openaire.observatory.indicator.dimension;

import java.util.List;

public record DimensionMemberPage(List<DimensionMember> members) {
    public DimensionMemberPage {
        members = List.copyOf(members);
    }
}
