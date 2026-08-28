package eu.openaire.observatory.indicator.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViewSelectorTest {

    @Test
    void requiresANonBlankDimension() {
        assertThatThrownBy(() -> new ViewSelector(null, null, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ViewSelector("  ", null, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalisesNullMembersToAnEmptyList() {
        assertThat(new ViewSelector("accessStatus", null, null).members()).isEmpty();
    }

    @Test
    void defensivelyCopiesMembers() {
        List<String> members = new ArrayList<>(List.of("OA", "CLOSED"));
        ViewSelector selector = new ViewSelector("accessStatus", "Access type", members);

        members.add("EMBARGOED");

        assertThat(selector.members()).containsExactly("OA", "CLOSED");
    }
}
