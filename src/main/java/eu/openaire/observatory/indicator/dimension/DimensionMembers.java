package eu.openaire.observatory.indicator.dimension;

import java.util.List;

public record DimensionMembers(List<DimensionMember> members) {
    public DimensionMembers {
        members = List.copyOf(members);
    }
}
