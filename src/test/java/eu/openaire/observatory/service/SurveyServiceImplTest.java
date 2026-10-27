package eu.openaire.observatory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.domain.History;
import eu.openaire.observatory.domain.HistoryEntry;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.SurveyAnswerRevisionsAggregation;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.dto.EditorDTO;
import eu.openaire.observatory.dto.HistoryActionDTO;
import eu.openaire.observatory.dto.HistoryDTO;
import eu.openaire.observatory.dto.HistoryEntryDTO;
import eu.openaire.observatory.permissions.PermissionService;
import eu.openaire.observatory.utils.OidcTestUtils;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.catalogue.ui.domain.ModelConfiguration;
import gr.uoa.di.madgik.catalogue.ui.domain.Section;
import gr.uoa.di.madgik.catalogue.ui.domain.UiField;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyServiceImplTest {

    @Mock
    private CrudService<Stakeholder> stakeholderCrudService;
    @Mock
    private CrudService<SurveyAnswer> surveyAnswerCrudService;
    @Mock
    private GenericResourceService genericResourceService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private ModelService modelService;
    @Mock
    private UserService userService;
    @Mock
    private CacheService<String, SurveyAnswerRevisionsAggregation> cacheService;
    @Mock
    private EmailSurveyService emailSurveyService;
    @Mock
    private SurveySettingsService surveySettingsService;

    private SurveyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new SurveyServiceImpl(
                stakeholderCrudService,
                surveyAnswerCrudService,
                genericResourceService,
                permissionService,
                modelService,
                userService,
                new ObjectMapper(),
                cacheService,
                emailSurveyService,
                surveySettingsService,
                new SurveyAnswerLocks()
        ));
    }

    @Test
    void generateStakeholderAnswerReusesExistingAnswer() {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId("sh-1");
        Model survey = new Model();
        survey.setId("survey-1");
        SurveyAnswer existing = new SurveyAnswer();
        existing.setId("sa-1");

        when(stakeholderCrudService.get("sh-1")).thenReturn(stakeholder);
        when(genericResourceService.get("model", "survey-1")).thenReturn(survey);
        doReturn(existing).when(service).getLatest("survey-1", "sh-1");

        SurveyAnswer result = service.generateStakeholderAnswer("sh-1", "survey-1", oidcAuthentication());

        assertSame(existing, result);
    }

    @Test
    void generateStakeholderAnswerCreatesNewAnswerWhenMissing() {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId("sh-1");
        stakeholder.setType("ai");
        Model survey = new Model();
        survey.setId("survey-1");
        survey.setType("ai");

        when(stakeholderCrudService.get("sh-1")).thenReturn(stakeholder);
        when(genericResourceService.get("model", "survey-1")).thenReturn(survey);
        doReturn(null).when(service).getLatest("survey-1", "sh-1");
        doReturn("manager").when(service).getUserRole(org.mockito.ArgumentMatchers.any(Authentication.class), org.mockito.ArgumentMatchers.eq("sh-1"));
        when(surveyAnswerCrudService.add(org.mockito.ArgumentMatchers.any(SurveyAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SurveyAnswer result = service.generateStakeholderAnswer("sh-1", "survey-1", oidcAuthentication());

        assertEquals("sh-1", result.getStakeholderId());
        assertEquals("survey-1", result.getSurveyId());
        assertEquals("ai", result.getType());
        assertEquals(1, result.getHistory().getEntries().size());
        assertEquals(History.HistoryAction.CREATED, result.getHistory().getEntries().getFirst().getAction());
        assertTrue(result.getMetadata().getCreatedBy().contains("@"));
    }

    @Test
    void setAnswerValidatedReturnsSameAnswerWhenStateUnchanged() throws ResourceNotFoundException {
        SurveyAnswer surveyAnswer = new SurveyAnswer();
        surveyAnswer.setId("sa-1");
        surveyAnswer.setStakeholderId("sh-1");
        surveyAnswer.setValidated(true);

        when(surveyAnswerCrudService.get("sa-1")).thenReturn(surveyAnswer);
        doReturn("manager").when(service).getUserRole(org.mockito.ArgumentMatchers.any(Authentication.class), org.mockito.ArgumentMatchers.eq("sh-1"));

        SurveyAnswer result = service.setAnswerValidated("sa-1", true, oidcAuthentication());

        assertSame(surveyAnswer, result);
        verify(surveyAnswerCrudService).get("sa-1");
        verify(emailSurveyService, never()).notifyAnswerValidated(any(), any());
    }

    // ── validation notification ──────────────────────────────────────────────

    /** Builds a not-yet-validated answer and stubs everything {@code validateAnswer} needs. */
    private SurveyAnswer stubAnswerForValidation(boolean currentlyValidated) throws ResourceNotFoundException {
        SurveyAnswer surveyAnswer = new SurveyAnswer();
        surveyAnswer.setId("sa-1");
        surveyAnswer.setStakeholderId("sh-1");
        surveyAnswer.setSurveyId("survey-1");
        surveyAnswer.setType("country");
        surveyAnswer.setValidated(currentlyValidated);

        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId("sh-1");
        stakeholder.setType("country");

        when(surveyAnswerCrudService.get("sa-1")).thenReturn(surveyAnswer);
        when(stakeholderCrudService.get("sh-1")).thenReturn(stakeholder);
        when(surveyAnswerCrudService.update(eq("sa-1"), any(SurveyAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        doReturn("manager").when(service).getUserRole(org.mockito.ArgumentMatchers.any(Authentication.class), org.mockito.ArgumentMatchers.eq("sh-1"));
        return surveyAnswer;
    }

    @Test
    void setAnswerValidatedNotifiesCoordinatorsWhenAnswerBecomesValidated() throws ResourceNotFoundException {
        stubAnswerForValidation(false);
        when(surveySettingsService.getByType("country"))
                .thenReturn(new eu.openaire.observatory.domain.SurveySettings().setNotifyOnValidation(true));

        SurveyAnswer result = service.setAnswerValidated("sa-1", true, oidcAuthentication());

        assertTrue(result.isValidated());
        ArgumentCaptor<SurveyAnswer> answerCaptor = ArgumentCaptor.forClass(SurveyAnswer.class);
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSurveyService).notifyAnswerValidated(answerCaptor.capture(), userCaptor.capture());
        assertEquals("sa-1", answerCaptor.getValue().getId());
        assertTrue(userCaptor.getValue().contains("@"));
    }

    /**
     * getByType throws rather than returning null when no settings record exists — which is the
     * default state of every survey type. Treating that as "do not notify" silenced the feature
     * entirely, so this is the regression test for it.
     */
    @Test
    void setAnswerValidatedNotifiesWhenNoSettingsRecordExists() throws ResourceNotFoundException {
        stubAnswerForValidation(false);
        when(surveySettingsService.getByType("country")).thenThrow(new ResourceNotFoundException());

        SurveyAnswer result = service.setAnswerValidated("sa-1", true, oidcAuthentication());

        assertTrue(result.isValidated());
        verify(emailSurveyService).notifyAnswerValidated(any(), any());
    }

    @Test
    void setAnswerValidatedDoesNotNotifyWhenSettingsDisableIt() throws ResourceNotFoundException {
        stubAnswerForValidation(false);
        when(surveySettingsService.getByType("country"))
                .thenReturn(new eu.openaire.observatory.domain.SurveySettings().setNotifyOnValidation(false));

        service.setAnswerValidated("sa-1", true, oidcAuthentication());

        verify(emailSurveyService, never()).notifyAnswerValidated(any(), any());
    }

    @Test
    void setAnswerValidatedDoesNotNotifyOnInvalidation() throws ResourceNotFoundException {
        stubAnswerForValidation(true);

        SurveyAnswer result = service.setAnswerValidated("sa-1", false, oidcAuthentication());

        assertFalse(result.isValidated());
        verify(emailSurveyService, never()).notifyAnswerValidated(any(), any());
    }

    @Test
    void setAnswerValidatedSucceedsWhenNotificationFails() throws ResourceNotFoundException {
        stubAnswerForValidation(false);
        when(surveySettingsService.getByType("country")).thenThrow(new RuntimeException("registry down"));

        SurveyAnswer result = service.setAnswerValidated("sa-1", true, oidcAuthentication());

        assertTrue(result.isValidated());
    }

    // enrichHistory tests — exercised via getHistory(), which calls enrichHistory() internally

    @Test
    void getHistorySetsFulnameToUnknownWhenEditorEmailIsNull() {
        HistoryDTO historyDTO = historyWithEditors(new EditorDTO(null, "manager", new Date()));
        when(surveyAnswerCrudService.getHistory(eq("sa-1"), any())).thenReturn(historyDTO);

        HistoryDTO result = service.getHistory("sa-1");

        assertThat(result.getEntries().getFirst().getEditors().getFirst().getFullname()).isEqualTo("unknown");
        verifyNoInteractions(userService);
    }

    @Test
    void getHistorySetsFulnameToUnknownWhenEditorEmailIsEmpty() {
        HistoryDTO historyDTO = historyWithEditors(new EditorDTO("", "manager", new Date()));
        when(surveyAnswerCrudService.getHistory(eq("sa-1"), any())).thenReturn(historyDTO);

        HistoryDTO result = service.getHistory("sa-1");

        assertThat(result.getEntries().getFirst().getEditors().getFirst().getFullname()).isEqualTo("unknown");
        verifyNoInteractions(userService);
    }

    @Test
    void getHistoryResolvesFullnameFromUserServiceWhenEmailIsPresent() {
        HistoryDTO historyDTO = historyWithEditors(new EditorDTO("alice@example.com", "manager", new Date()));
        User alice = new User();
        alice.setEmail("alice@example.com");
        alice.setFullname("Alice Smith");
        when(surveyAnswerCrudService.getHistory(eq("sa-1"), any())).thenReturn(historyDTO);
        when(userService.get("alice@example.com")).thenReturn(alice);

        HistoryDTO result = service.getHistory("sa-1");

        assertThat(result.getEntries().getFirst().getEditors().getFirst().getFullname()).isEqualTo("Alice Smith");
    }

    @Test
    void getHistorySetsFulnameToUnknownWhenUserNotFoundInRegistry() {
        HistoryDTO historyDTO = historyWithEditors(new EditorDTO("ghost@example.com", "manager", new Date()));
        when(surveyAnswerCrudService.getHistory(eq("sa-1"), any())).thenReturn(historyDTO);
        when(userService.get("ghost@example.com")).thenThrow(new ResourceNotFoundException("ghost@example.com", "user"));

        HistoryDTO result = service.getHistory("sa-1");

        assertThat(result.getEntries().getFirst().getEditors().getFirst().getFullname()).isEqualTo("unknown");
    }

    // ── import answers from a previous survey ────────────────────────────────

    /**
     * Last year's answer used by the import tests. Against {@link #currentModel()}:
     * Q1 is active with a deprecated sub-field (Q1-2) inside a repeatable group, Q2 and Q5 are paused,
     * Q3 and the whole "Old" section are deleted, Q4 is active with a deprecated sub-field (Q4-1),
     * Q6 is new, and "Unknown" is not described by any model.
     */
    private static final String PREVIOUS_ANSWER = """
            {
              "Policies": {
                "Q1": {
                  "Q1-0": "Yes",
                  "Q1-1-0": [
                    {"Q1-1": "a", "Q1-2": "Yes", "Q1-2-1": ["https://a.org"]},
                    {"Q1-1": "b", "Q1-2": "No"}
                  ]
                },
                "Q2": {"Q2-0": "Yes", "Q2-1": "paused text"},
                "Q3": {"Q3-0": "deleted"},
                "Q4": {"Q4-0": "No", "Q4-1": "deprecated text"},
                "Q5": {},
                "Unknown": "keep me"
              },
              "Old": {"Q8": {"Q8-0": "gone"}},
              "Feedback": {"Q9": null}
            }""";

    private static Model previousModel() {
        Model model = new Model();
        model.setId("m-2024");
        model.setName("Survey 2024");
        model.setSections(List.of(
                section("Policies", section("Data",
                        field("Q1", "question",
                                field("Q1-0"),
                                field("Q1-1-0", null, field("Q1-1"), field("Q1-2"), field("Q1-2-1"))),
                        field("Q2", "question", field("Q2-0"), field("Q2-1")),
                        field("Q3", "question", field("Q3-0")),
                        field("Q4", "question", field("Q4-0"), field("Q4-1")),
                        field("Q5", "external"))),
                section("Old", section("Old questions", field("Q8", "question", field("Q8-0")))),
                section("Feedback", section("Feedback", field("Q9", "question")))));
        return model;
    }

    private static Model currentModel() {
        Model model = new Model();
        model.setId("m-2025");
        model.setName("Survey 2025");
        model.setConfiguration(new ModelConfiguration().setPrefillable(true).setImportFrom(List.of("m-2024")));
        model.setSections(List.of(
                section("Policies", section("Data",
                        field("Q1", "question",
                                field("Q1-0"),
                                field("Q1-1-0", null, field("Q1-1"), deprecated(field("Q1-2")), field("Q1-2-1"))),
                        field("Q2", "paused", field("Q2-0"), field("Q2-1")),
                        field("Q4", "question", field("Q4-0"), deprecated(field("Q4-1"))),
                        field("Q5", "paused"),
                        field("Q6", "question", field("Q6-0")))),
                section("Feedback", section("Feedback", field("Q9", "question")))));
        return model;
    }

    private static UiField field(String name) {
        return field(name, null);
    }

    private static UiField field(String name, String kind, UiField... subFields) {
        UiField field = new UiField();
        field.setName(name);
        field.setKind(kind);
        field.setSubFields(List.of(subFields));
        return field;
    }

    private static UiField deprecated(UiField field) {
        field.setDeprecated(true);
        return field;
    }

    private static Section section(String name, UiField... fields) {
        Section section = new Section();
        section.setName(name);
        section.setFields(List.of(fields));
        return section;
    }

    private static Section section(String name, Section... subSections) {
        Section section = new Section();
        section.setName(name);
        section.setSubSections(List.of(subSections));
        return section;
    }

    /** Runs an import of {@code previous} into a 2025 answer and returns what gets saved. */
    private SurveyAnswer importAnswer(SurveyAnswer previous) throws ResourceNotFoundException {
        SurveyAnswer current = new SurveyAnswer();
        current.setId("sa-2025");
        current.setSurveyId("m-2025");
        current.setStakeholderId("sh-1");

        when(modelService.get("m-2024")).thenReturn(previousModel());
        when(modelService.get("m-2025")).thenReturn(currentModel());
        when(surveyAnswerCrudService.get("sa-2025")).thenReturn(current);
        when(surveyAnswerCrudService.update(eq("sa-2025"), any(SurveyAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        doReturn(previous).when(service).getLatest("m-2024", "sh-1");
        doReturn("manager").when(service).getUserRole(any(Authentication.class), eq("sh-1"));

        return service.importAnswer("sa-2025", "m-2024", oidcAuthentication());
    }

    private static SurveyAnswer previousAnswer() {
        SurveyAnswer previous = new SurveyAnswer();
        previous.setId("sa-2024");
        previous.setSurveyId("m-2024");
        previous.setStakeholderId("sh-1");
        previous.setAnswer(json(PREVIOUS_ANSWER));
        return previous;
    }

    private static JSONObject json(String json) {
        return (JSONObject) JSONValue.parse(json);
    }

    private JSONObject importedPolicies() throws ResourceNotFoundException {
        return (JSONObject) importAnswer(previousAnswer()).getAnswer().get("Policies");
    }

    @Test
    void importKeepsActiveAnswers() throws ResourceNotFoundException {
        JSONObject policies = importedPolicies();

        assertEquals("Yes", ((JSONObject) policies.get("Q1")).get("Q1-0"));
        assertEquals("No", ((JSONObject) policies.get("Q4")).get("Q4-0"));
    }

    @Test
    void importRemovesPausedQuestionWithAllItsSubQuestions() throws ResourceNotFoundException {
        JSONObject policies = importedPolicies();

        assertFalse(policies.containsKey("Q2"));
        assertFalse(policies.containsKey("Q5"));
    }

    @Test
    void importRemovesDeprecatedSubQuestionAndKeepsItsSiblings() throws ResourceNotFoundException {
        JSONObject q4 = (JSONObject) importedPolicies().get("Q4");

        assertEquals(json("{\"Q4-0\": \"No\"}"), q4);
    }

    @Test
    void importRemovesDeprecatedSubQuestionFromEveryRepeatableEntry() throws ResourceNotFoundException {
        JSONObject q1 = (JSONObject) importedPolicies().get("Q1");

        assertEquals(JSONValue.parse("""
                [
                  {"Q1-1": "a", "Q1-2-1": ["https://a.org"]},
                  {"Q1-1": "b"}
                ]"""), q1.get("Q1-1-0"));
    }

    @Test
    void importRemovesDeletedQuestionsAndSections() throws ResourceNotFoundException {
        JSONObject answer = importAnswer(previousAnswer()).getAnswer();

        assertFalse(((JSONObject) answer.get("Policies")).containsKey("Q3"));
        assertFalse(answer.containsKey("Old"));
    }

    @Test
    void importLeavesNewQuestionsEmptyAndKeepsUnknownKeys() throws ResourceNotFoundException {
        JSONObject answer = importAnswer(previousAnswer()).getAnswer();

        assertFalse(((JSONObject) answer.get("Policies")).containsKey("Q6"));
        assertEquals("keep me", ((JSONObject) answer.get("Policies")).get("Unknown"));
        assertEquals(json("{\"Q9\": null}"), answer.get("Feedback"));
    }

    @Test
    void importDoesNotModifyThePreviousAnswer() throws ResourceNotFoundException {
        SurveyAnswer previous = previousAnswer();

        importAnswer(previous);

        assertEquals(json(PREVIOUS_ANSWER), previous.getAnswer());
    }

    @Test
    void importWithoutPreviousAnswerSetsEmptyAnswer() throws ResourceNotFoundException {
        SurveyAnswer result = importAnswer(null);

        assertEquals(new JSONObject(), result.getAnswer());
    }

    @Test
    void importAddsImportedHistoryEntry() throws ResourceNotFoundException {
        SurveyAnswer result = importAnswer(previousAnswer());

        HistoryEntry last = result.getHistory().getEntries().getLast();
        assertEquals(History.HistoryAction.IMPORTED, last.getAction());
        assertEquals("Imported data from 'Survey 2024'", last.getComment());
    }

    private static HistoryDTO historyWithEditors(EditorDTO... editors) {
        HistoryEntryDTO entry = new HistoryEntryDTO();
        entry.setEditors(List.of(editors));
        entry.setAction(HistoryActionDTO.of(History.HistoryAction.UPDATED, null));
        return new HistoryDTO(List.of(entry));
    }

    private Authentication oidcAuthentication() {
        return OidcTestUtils.oidcAuthentication("user@example.org");
    }
}
