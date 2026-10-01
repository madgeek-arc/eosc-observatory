package eu.openaire.observatory.indicator.survey;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.service.SurveyService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PausedSurveyQuestionsTest {

    private static final String MODEL_JSON = """
        {"id":"m-eosc-sb-2025","name":"Survey 2025","sections":[{"id":"s1","name":"Practices","order":1,
          "subSections":[{"id":"s1a","name":"Services","order":1,"fields":[
            {"id":"1","name":"Question76","kind":"question","typeInfo":{"type":"composite"},
             "form":{"description":{"text":"[Question is paused]"}},
             "subFields":[{"id":"2","name":"Question76-0","typeInfo":{"type":"number"},"form":{"description":{"text":"Glossary"}}}]},
            {"id":"3","name":"Question56","kind":"question","typeInfo":{"type":"composite"},"form":{"description":{"text":null}}},
            {"id":"4","name":"Question5","kind":"question","typeInfo":{"type":"composite"}}
          ]}]}]}
        """;

    @Test
    void reportsFieldsWhoseDescriptionCarriesThePausedMarker() throws Exception {
        Model model = new ObjectMapper().readValue(MODEL_JSON, Model.class);

        assertThat(PausedSurveyQuestions.pausedQuestionCodes(model)).containsExactly("Question76");
    }

    @Test
    void readsTheModelOfTheRequestedYear() throws Exception {
        SurveyService surveyService = mock(SurveyService.class);
        Model model2025 = new ObjectMapper().readValue(MODEL_JSON, Model.class);
        Model model2024 = new ObjectMapper().readValue(MODEL_JSON.replace("Survey 2025", "Survey 2024")
            .replace("[Question is paused]", ""), Model.class);
        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(List.of(model2024, model2025));
        when(surveyService.getByType(any(FacetFilter.class), eq("eosc-sb"))).thenReturn(browsing);
        PausedSurveyQuestions paused = new PausedSurveyQuestions(surveyService);

        assertThat(paused.pausedQuestionCodes(2025)).isEqualTo(Set.of("Question76"));
        assertThat(paused.pausedQuestionCodes(2024)).isEmpty();
    }

    @Test
    void aSnapshotAnswersLikeIsPausedAndIsUnaffectedByLaterChanges() {
        java.util.concurrent.atomic.AtomicReference<Set<String>> current =
            new java.util.concurrent.atomic.AtomicReference<>(Set.of("Question30"));
        PausedSurveyQuestions paused = new PausedSurveyQuestions(year -> year == 2025 ? current.get() : Set.of());

        var snapshot = paused.snapshot(List.of(2024, 2025));
        current.set(Set.of());

        assertThat(snapshot.isPaused(2025, "Question30")).isTrue();
        assertThat(snapshot.isPaused(2025, "Question30.1")).isTrue();
        assertThat(snapshot.isPaused(2025, "Question300")).isFalse();
        assertThat(snapshot.isPaused(2024, "Question30")).isFalse();
        assertThat(snapshot.isPaused(2026, "Question30")).isFalse();
    }

    @Test
    void reportsNothingPausedWhenNoModelMatchesTheYear() {
        SurveyService surveyService = mock(SurveyService.class);
        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(List.of());
        when(surveyService.getByType(any(FacetFilter.class), eq("eosc-sb"))).thenReturn(browsing);

        assertThat(new PausedSurveyQuestions(surveyService).isPaused(2030, "Question76")).isFalse();
    }

    @Test
    void matchesTheModelByTheYearAtTheEndOfItsNameForEveryRealNameShape() throws Exception {
        SurveyService surveyService = mock(SurveyService.class);
        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(List.of(
            modelNamed("Survey on National Contributions to EOSC 2021", true),
            modelNamed("Survey on National Contributions to EOSC 2022 ", false),
            modelNamed("Survey on National Contributions to EOSC and Open Science 2025", true),
            modelNamed("Survey 20250", true)));
        when(surveyService.getByType(any(FacetFilter.class), eq("eosc-sb"))).thenReturn(browsing);
        PausedSurveyQuestions paused = new PausedSurveyQuestions(surveyService);

        assertThat(paused.pausedQuestionCodes(2021)).containsExactly("Question76");
        assertThat(paused.pausedQuestionCodes(2022)).isEmpty();
        assertThat(paused.pausedQuestionCodes(2025)).containsExactly("Question76");
        assertThat(paused.pausedQuestionCodes(2020)).isEmpty();
    }

    @Test
    void readsTheSurveyModelsOnceForAMultiYearSnapshot() throws Exception {
        SurveyService surveyService = mock(SurveyService.class);
        Browsing<Model> browsing = new Browsing<>();
        browsing.setResults(List.of(modelNamed("Survey 2024", false), modelNamed("Survey 2025", true)));
        when(surveyService.getByType(any(FacetFilter.class), eq("eosc-sb"))).thenReturn(browsing);
        PausedSurveyQuestions paused = new PausedSurveyQuestions(surveyService);

        var snapshot = paused.snapshot(List.of(2022, 2023, 2024, 2025));
        paused.snapshot(List.of(2022, 2025));

        assertThat(snapshot.isPaused(2025, "Question76")).isTrue();
        assertThat(snapshot.isPaused(2024, "Question76")).isFalse();
        verify(surveyService, times(1)).getByType(any(FacetFilter.class), eq("eosc-sb"));
    }

    @Test
    void aFailedReadReportsNothingPausedWithoutCachingTheFailureOrRetryingImmediately() throws Exception {
        SurveyService surveyService = mock(SurveyService.class);
        when(surveyService.getByType(any(FacetFilter.class), eq("eosc-sb"))).thenThrow(new IllegalStateException("registry down"));
        PausedSurveyQuestions paused = new PausedSurveyQuestions(surveyService);

        assertThat(paused.pausedQuestionCodes(2025)).isEmpty();
        assertThat(paused.snapshot(List.of(2024, 2025)).isPaused(2025, "Question76")).isFalse();

        verify(surveyService, times(1)).getByType(any(FacetFilter.class), eq("eosc-sb"));
    }

    private static Model modelNamed(String name, boolean paused) throws Exception {
        String json = MODEL_JSON.replace("Survey 2025", name);
        return new ObjectMapper().readValue(paused ? json : json.replace("[Question is paused]", ""), Model.class);
    }
}
