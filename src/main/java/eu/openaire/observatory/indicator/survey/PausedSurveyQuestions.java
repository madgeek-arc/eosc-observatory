package eu.openaire.observatory.indicator.survey;

import eu.openaire.observatory.service.SurveyService;
import gr.uoa.di.madgik.catalogue.ui.domain.Form;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.catalogue.ui.domain.Section;
import gr.uoa.di.madgik.catalogue.ui.domain.StyledString;
import gr.uoa.di.madgik.catalogue.ui.domain.UiField;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

/**
 * Resolves which survey questions are paused for a reference year. A question is paused when its field
 * in the year's "eosc-sb" survey model carries {@value #PAUSED_MARKER} in its form description; a
 * paused question is reported by the name of that field (e.g. {@code Question76}), which is also the
 * question code used in the survey stats-tool's named queries.
 *
 * <p>The survey model for a year is the "eosc-sb" model {@link SurveyEditions} lists for it. When models
 * can't be read, the last known answer keeps being served and the read is retried shortly; with no
 * previous answer, no question is reported as paused and nothing is cached. Successful answers are
 * cached briefly so edits to a model are picked up without a restart.
 */
@Component
public class PausedSurveyQuestions {

    static final String PAUSED_MARKER = "[Question is paused]";

    private static final Logger logger = LoggerFactory.getLogger(PausedSurveyQuestions.class);
    private static final String EOSC_SB_SURVEY_TYPE = "eosc-sb";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration RETRY_AFTER_FAILURE = Duration.ofSeconds(15);

    /** Reads the paused question codes of every given year in one go; throws when they can't be read. */
    private interface Loader {
        Map<Integer, Set<String>> load(Set<Integer> years);
    }

    private final Loader loader;
    private final Map<Integer, CachedEntry> cache = new ConcurrentHashMap<>();
    private long retryNotBeforeNanos;
    private boolean retryScheduled;

    @Autowired
    public PausedSurveyQuestions(SurveyService surveyService) {
        this.loader = years -> loadFromSurveyModels(surveyService, years);
    }

    PausedSurveyQuestions(IntFunction<Set<String>> perYearLoader) {
        this.loader = years -> {
            Map<Integer, Set<String>> byYear = new HashMap<>();
            years.forEach(year -> byYear.put(year, perYearLoader.apply(year)));
            return byYear;
        };
    }

    /** An instance that never reports a paused question. */
    public static PausedSurveyQuestions none() {
        return new PausedSurveyQuestions(year -> Set.of());
    }

    /** Whether the question, or the parent question of a sub-question code such as {@code Question30.1}, is paused. */
    public boolean isPaused(int year, String questionCode) {
        return pausedQuestionCodes(year).contains(parentCode(questionCode));
    }

    public Set<String> pausedQuestionCodes(int year) {
        return refreshed(List.of(year)).getOrDefault(year, Set.of());
    }

    /** The paused questions of each given year, read once so a caller sees one consistent answer. */
    public Snapshot snapshot(Collection<Integer> years) {
        Map<Integer, Set<String>> byYear = new HashMap<>();
        refreshed(years).forEach((year, codes) -> byYear.put(year, Set.copyOf(codes)));
        return new Snapshot(Map.copyOf(byYear));
    }

    /** Paused question codes per year, as of the moment {@link #snapshot} was taken. */
    public record Snapshot(Map<Integer, Set<String>> pausedByYear) {

        /** Whether the question, or the parent question of a sub-question code such as {@code Question30.1}, is paused. */
        public boolean isPaused(int year, String questionCode) {
            return pausedByYear.getOrDefault(year, Set.of()).contains(parentCode(questionCode));
        }
    }

    private synchronized Map<Integer, Set<String>> refreshed(Collection<Integer> years) {
        long now = System.nanoTime();
        Set<Integer> stale = new HashSet<>();
        for (int year : years) {
            CachedEntry entry = cache.get(year);
            if (entry == null || now - entry.loadedAtNanos() > CACHE_TTL.toNanos()) {
                stale.add(year);
            }
        }
        if (!stale.isEmpty() && (!retryScheduled || now - retryNotBeforeNanos >= 0)) {
            try {
                loader.load(stale).forEach((year, codes) -> cache.put(year, new CachedEntry(codes, now)));
                retryScheduled = false;
            } catch (RuntimeException e) {
                logger.warn("Could not read the {} survey models for {}; keeping the last known paused questions",
                    EOSC_SB_SURVEY_TYPE, stale, e);
                retryScheduled = true;
                retryNotBeforeNanos = now + RETRY_AFTER_FAILURE.toNanos();
            }
        }
        Map<Integer, Set<String>> result = new HashMap<>();
        for (int year : years) {
            CachedEntry entry = cache.get(year);
            result.put(year, entry == null ? Set.of() : entry.codes());
        }
        return result;
    }

    private static String parentCode(String questionCode) {
        int subQuestionSeparator = questionCode.indexOf('.');
        return subQuestionSeparator < 0 ? questionCode : questionCode.substring(0, subQuestionSeparator);
    }

    static Set<String> pausedQuestionCodes(Model model) {
        Set<String> paused = new HashSet<>();
        collect(model.getSections(), paused);
        return paused;
    }

    private static Map<Integer, Set<String>> loadFromSurveyModels(SurveyService surveyService, Set<Integer> years) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        Map<Integer, Model> modelsByYear = new HashMap<>();
        for (Model model : surveyService.getByType(filter, EOSC_SB_SURVEY_TYPE).getResults()) {
            SurveyEditions.forSurveyId(model.getId())
                .ifPresent(edition -> modelsByYear.put(edition.referenceYear(), model));
        }
        Map<Integer, Set<String>> byYear = new HashMap<>();
        for (int year : years) {
            Model model = modelsByYear.get(year);
            if (model == null) {
                logger.debug("No {} survey model found for {}; no question is treated as paused", EOSC_SB_SURVEY_TYPE, year);
            }
            byYear.put(year, model == null ? Set.of() : pausedQuestionCodes(model));
        }
        return byYear;
    }

    private static void collect(List<Section> sections, Set<String> paused) {
        if (sections == null) {
            return;
        }
        for (Section section : sections) {
            collectFields(section.getFields(), paused);
            collect(section.getSubSections(), paused);
        }
    }

    private static void collectFields(List<UiField> fields, Set<String> paused) {
        if (fields == null) {
            return;
        }
        for (UiField field : fields) {
            if (isPaused(field)) {
                paused.add(field.getName());
            }
            collectFields(field.getSubFields(), paused);
        }
    }

    private static boolean isPaused(UiField field) {
        Form form = field.getForm();
        StyledString description = form == null ? null : form.getDescription();
        return description != null && description.getText() != null && description.getText().contains(PAUSED_MARKER);
    }

    private record CachedEntry(Set<String> codes, long loadedAtNanos) {
    }
}
