package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.service.StakeholderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryDimensionMemberProviderTest {

    @Mock
    private StakeholderService stakeholderService;

    private CountryDimensionMemberProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CountryDimensionMemberProvider(stakeholderService);
    }

    @Test
    void returnsAllCountriesWithDisplayLabelsWhenNoSearchText() {
        when(stakeholderService.getAllCountryCodes()).thenReturn(List.of("DE", "GR"));

        DimensionMemberPage page = provider.search("country", null, "", 100);

        assertThat(page.members()).containsExactly(
            new DimensionMember("DE", "Germany"),
            new DimensionMember("GR", "Greece")
        );
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        when(stakeholderService.getAllCountryCodes()).thenReturn(List.of("DE", "GR", "FR"));

        DimensionMemberPage page = provider.search("country", null, "ger", 100);

        assertThat(page.members()).containsExactly(new DimensionMember("DE", "Germany"));
    }

    @Test
    void truncatesToLimit() {
        when(stakeholderService.getAllCountryCodes()).thenReturn(List.of("DE", "GR", "FR"));

        DimensionMemberPage page = provider.search("country", null, "", 2);

        assertThat(page.members()).hasSize(2);
    }

    @Test
    void throwsForUnknownDimension() {
        assertThatThrownBy(() -> provider.search("industry", null, "", 100))
            .isInstanceOf(UnknownDimensionException.class);
    }
}
