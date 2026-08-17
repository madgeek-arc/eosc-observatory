package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireQueryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldOfScienceDimensionMemberProviderTest {

    @Mock
    private OpenAireDimensionMemberFetcher fetcher;

    @Test
    void dimensionCodeIsFieldOfScience() {
        assertThat(new FieldOfScienceDimensionMemberProvider(fetcher).dimensionCode())
            .isEqualTo(IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION);
    }

    @Test
    void alwaysFetchesThePublicationScopedListRegardlessOfIndicatorCode() {
        var provider = new FieldOfScienceDimensionMemberProvider(fetcher);
        lenient().when(fetcher.fetchCached(anyString(), anyString(), anyString()))
            .thenReturn(new DimensionMembers(List.of(new DimensionMember("01 natural sciences", "01 natural sciences"))));

        provider.search(IndicatorCodes.DATASETS_COUNT, "", 100);

        verify(fetcher).fetchCached(
            "dimension-members:fieldOfScience:" + OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION,
            IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);
    }

    @Test
    void filtersAndLimitsTheFetchedList() {
        var provider = new FieldOfScienceDimensionMemberProvider(fetcher);
        when(fetcher.fetchCached(anyString(), anyString(), anyString())).thenReturn(new DimensionMembers(List.of(
            new DimensionMember("01 natural sciences", "01 natural sciences"),
            new DimensionMember("02 engineering and technology", "02 engineering and technology")
        )));

        DimensionMembers page = provider.search(null, "natural", 100);

        assertThat(page.members()).containsExactly(new DimensionMember("01 natural sciences", "01 natural sciences"));
    }
}
