package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.service.SurveyService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyYearDimensionMemberProviderTest {

    private static final Date PAST = Date.from(Instant.now().minus(30, ChronoUnit.DAYS));
    private static final Date FUTURE = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));

    @Mock
    private SurveyService surveyService;

    private SurveyYearDimensionMemberProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SurveyYearDimensionMemberProvider(surveyService);
    }

    @Test
    void countsConcludedSurveysFrom2021Upwards() {
        stubSurveys(
            concluded(), concluded(), concluded(), // -> 2021, 2022, 2023
            active()                               // 2024 still collecting
        );

        DimensionMembers members = provider.search(null, "", 100);

        assertThat(members.members()).containsExactly(
            new DimensionMember("2021", "2021"),
            new DimensionMember("2022", "2022"),
            new DimensionMember("2023", "2023")
        );
    }

    @Test
    void aSingleConcludedSurveyMapsToJust2021() {
        stubSurveys(concluded());

        assertThat(provider.search(null, "", 100).members())
            .containsExactly(new DimensionMember("2021", "2021"));
    }

    @Test
    void noConcludedSurveysYieldsAnEmptyList() {
        stubSurveys(active(), active());

        assertThat(provider.search(null, "", 100).members()).isEmpty();
    }

    @Test
    void inactiveSurveyWithNoCloseDateCountsAsConcluded() {
        Model model = new Model();
        model.setActive(false);
        model.setSubmissionCloseAt(null);
        stubSurveys(model);

        assertThat(provider.search(null, "", 100).members())
            .containsExactly(new DimensionMember("2021", "2021"));
    }

    @Test
    void inactiveSurveyWithAFutureCloseDateIsNotConcluded() {
        Model model = new Model();
        model.setActive(false);
        model.setSubmissionCloseAt(FUTURE);
        stubSurveys(model);

        assertThat(provider.search(null, "", 100).members()).isEmpty();
    }

    @Test
    void activeSurveyIsNeverConcludedEvenWithAPastCloseDate() {
        Model model = new Model();
        model.setActive(true);
        model.setSubmissionCloseAt(PAST);
        stubSurveys(model);

        assertThat(provider.search(null, "", 100).members()).isEmpty();
    }

    @Test
    void filtersByCaseInsensitiveSearchText() {
        stubSurveys(concluded(), concluded(), concluded(), concluded(), concluded()); // 2021..2025

        assertThat(provider.search(null, "2023", 100).members())
            .containsExactly(new DimensionMember("2023", "2023"));
    }

    @Test
    void truncatesToLimit() {
        stubSurveys(concluded(), concluded(), concluded(), concluded(), concluded());

        assertThat(provider.search(null, "", 2).members()).hasSize(2);
    }

    @Test
    void nullResultListYieldsAnEmptyMemberList() {
        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(null);
        when(surveyService.getByType(any(FacetFilter.class), eq("eosc-sb"))).thenReturn(browsing);

        assertThat(provider.search(null, "", 100).members()).isEmpty();
    }

    private static Model concluded() {
        Model model = new Model();
        model.setActive(false);
        model.setSubmissionCloseAt(PAST);
        return model;
    }

    private static Model active() {
        Model model = new Model();
        model.setActive(true);
        model.setSubmissionCloseAt(null);
        return model;
    }

    private void stubSurveys(Model... models) {
        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(List.of(models));
        when(surveyService.getByType(any(FacetFilter.class), eq("eosc-sb"))).thenReturn(browsing);
    }
}
