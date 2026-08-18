package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireQueryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationDimensionMemberProviderTest {

    @Mock
    private OpenAireDimensionMemberFetcher fetcher;

    @Test
    void dimensionCodeIsClassification() {
        assertThat(new ClassificationDimensionMemberProvider(fetcher).dimensionCode())
            .isEqualTo(IndicatorCodes.CLASSIFICATION_DIMENSION);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entityScopedListCases")
    void indicatorFetchesItsEntityScopedList(String indicatorCode, String resultType, DimensionMember sampleMember) {
        var provider = new ClassificationDimensionMemberProvider(fetcher);
        when(fetcher.fetchCached(
            eq("dimension-members:classification:" + resultType),
            eq(IndicatorCodes.CLASSIFICATION_DIMENSION), eq(resultType)
        )).thenReturn(new DimensionMembers(List.of(sampleMember)));

        DimensionMembers page = provider.search(indicatorCode, "", 100);

        assertThat(page.members()).containsExactly(sampleMember);
        verify(fetcher).fetchCached(
            "dimension-members:classification:" + resultType, IndicatorCodes.CLASSIFICATION_DIMENSION, resultType);
    }

    private static Stream<Arguments> entityScopedListCases() {
        return Stream.of(
            Arguments.of(IndicatorCodes.PUBLICATIONS_COUNT, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION,
                new DimensionMember("Article", "Article")),
            Arguments.of(IndicatorCodes.DATASETS_COUNT, OpenAireQueryBuilder.RESULT_TYPE_DATASET,
                new DimensionMember("Dataset", "Dataset")),
            Arguments.of(IndicatorCodes.SOFTWARE_COUNT, OpenAireQueryBuilder.RESULT_TYPE_SOFTWARE,
                new DimensionMember("Software", "Software"))
        );
    }

    @Test
    void nullIndicatorCodeDefaultsToThePublicationScopedList() {
        var provider = new ClassificationDimensionMemberProvider(fetcher);
        when(fetcher.fetchCached(
            eq("dimension-members:classification:" + OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION),
            eq(IndicatorCodes.CLASSIFICATION_DIMENSION), eq(OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION)
        )).thenReturn(new DimensionMembers(List.of()));

        provider.search(null, "", 100);

        verify(fetcher).fetchCached(
            "dimension-members:classification:" + OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION,
            IndicatorCodes.CLASSIFICATION_DIMENSION, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);
    }

    @Test
    void filtersAndLimitsTheFetchedList() {
        var provider = new ClassificationDimensionMemberProvider(fetcher);
        when(fetcher.fetchCached(eq("dimension-members:classification:" + OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION),
            eq(IndicatorCodes.CLASSIFICATION_DIMENSION), eq(OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION)))
            .thenReturn(new DimensionMembers(List.of(
                new DimensionMember("Article", "Article"), new DimensionMember("Book", "Book")
            )));

        DimensionMembers page = provider.search(IndicatorCodes.PUBLICATIONS_COUNT, "art", 100);

        assertThat(page.members()).containsExactly(new DimensionMember("Article", "Article"));
    }
}
