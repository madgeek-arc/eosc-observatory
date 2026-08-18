package eu.openaire.observatory.indicator.dimension;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Base for a {@link DimensionMemberProvider} whose fixed member list mirrors a closed Java enum's
 * constants one-to-one (e.g. the OpenAIRE filter-literal translation enums in the {@code openaire}
 * package) — deriving the list from the enum removes the risk of it drifting out of sync with the
 * enum by hand. The member code is always the constant's {@link Enum#name()}; the label defaults
 * to the same {@code name()} but can be overridden when the catalog needs different display copy
 * than the raw constant name (e.g. "Embargoed" instead of "EMBARGOED").
 */
abstract class EnumDimensionMemberProvider<E extends Enum<E>> implements DimensionMemberProvider {

    private final String dimensionCode;
    private final List<DimensionMember> members;

    protected EnumDimensionMemberProvider(String dimensionCode, Class<E> enumClass) {
        this(dimensionCode, enumClass, Enum::name);
    }

    protected EnumDimensionMemberProvider(String dimensionCode, Class<E> enumClass, Function<E, String> label) {
        this.dimensionCode = dimensionCode;
        this.members = Stream.of(enumClass.getEnumConstants())
            .map(constant -> new DimensionMember(constant.name(), label.apply(constant)))
            .toList();
    }

    @Override
    public String dimensionCode() {
        return dimensionCode;
    }

    @Override
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        return DimensionMemberSearch.filter(members, searchText, limit, false);
    }
}
