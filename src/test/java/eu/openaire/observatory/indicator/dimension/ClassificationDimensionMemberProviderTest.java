package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireQueryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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

    @Test
    void publicationsCountIndicatorFetchesThePublicationScopedList() {
        var provider = new ClassificationDimensionMemberProvider(fetcher);
        when(fetcher.fetchCached(
            eq("dimension-members:classification:" + OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION),
            eq(IndicatorCodes.CLASSIFICATION_DIMENSION), eq(OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION)
        )).thenReturn(new DimensionMembers(List.of(new DimensionMember("Article", "Article"))));

        DimensionMembers page = provider.search(IndicatorCodes.PUBLICATIONS_COUNT, "", 100);

        assertThat(page.members()).containsExactly(new DimensionMember("Article", "Article"));
        verify(fetcher).fetchCached(
            "dimension-members:classification:" + OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION,
            IndicatorCodes.CLASSIFICATION_DIMENSION, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);
    }

    @Test
    void datasetsCountIndicatorFetchesTheDatasetScopedList() {
        var provider = new ClassificationDimensionMemberProvider(fetcher);
        when(fetcher.fetchCached(
            eq("dimension-members:classification:" + OpenAireQueryBuilder.RESULT_TYPE_DATASET),
            eq(IndicatorCodes.CLASSIFICATION_DIMENSION), eq(OpenAireQueryBuilder.RESULT_TYPE_DATASET)
        )).thenReturn(new DimensionMembers(List.of(new DimensionMember("Dataset", "Dataset"))));

        DimensionMembers page = provider.search(IndicatorCodes.DATASETS_COUNT, "", 100);

        assertThat(page.members()).containsExactly(new DimensionMember("Dataset", "Dataset"));
    }

    @Test
    void softwareCountIndicatorFetchesTheSoftwareScopedList() {
        var provider = new ClassificationDimensionMemberProvider(fetcher);
        when(fetcher.fetchCached(
            eq("dimension-members:classification:" + OpenAireQueryBuilder.RESULT_TYPE_SOFTWARE),
            eq(IndicatorCodes.CLASSIFICATION_DIMENSION), eq(OpenAireQueryBuilder.RESULT_TYPE_SOFTWARE)
        )).thenReturn(new DimensionMembers(List.of(new DimensionMember("Software", "Software"))));

        DimensionMembers page = provider.search(IndicatorCodes.SOFTWARE_COUNT, "", 100);

        assertThat(page.members()).containsExactly(new DimensionMember("Software", "Software"));
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
