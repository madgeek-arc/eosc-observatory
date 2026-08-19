package eu.openaire.observatory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.domain.History;
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
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
                cacheService
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

    // getQuestionAnswersByCountry — local equivalent of the survey-stats-tool's
    // "eosc.sb.<year>.<questionCode>" named-query row fetch (jsonb path answer.*.<code>.<code>-0)

    @Test
    void getQuestionAnswersByCountryReadsValueNestedUnderQuestionCodeInAnySection() {
        SurveyAnswer answer = validatedAnswer("sa-1", "sh-1", sectionWithQuestion("Question6", "Yes"));

        mockSurveyAnswers(answer);
        mockStakeholders(stakeholder("sh-1", "GR"));

        List<List<String>> rows = service.getQuestionAnswersByCountry("m-eosc-sb-2024", "Question6");

        assertThat(rows).containsExactly(List.of("GR", "Yes"));
    }

    @Test
    void getQuestionAnswersByCountryReturnsNullWhenQuestionSubfieldMissing() {
        JSONObject question = new JSONObject();
        JSONObject section = new JSONObject();
        section.put("Question6", question); // no "Question6-0" entry
        SurveyAnswer answer = validatedAnswer("sa-1", "sh-1", section);

        mockSurveyAnswers(answer);
        mockStakeholders(stakeholder("sh-1", "DE"));

        List<List<String>> rows = service.getQuestionAnswersByCountry("m-eosc-sb-2024", "Question6");

        assertThat(rows.get(0).get(0)).isEqualTo("DE");
        assertThat(rows.get(0).get(1)).isNull();
    }

    @Test
    void getQuestionAnswersByCountryReturnsNullWhenQuestionNotPresentInAnySection() {
        SurveyAnswer answer = validatedAnswer("sa-1", "sh-1", sectionWithQuestion("Question7", "No"));

        mockSurveyAnswers(answer);
        mockStakeholders(stakeholder("sh-1", "FR"));

        List<List<String>> rows = service.getQuestionAnswersByCountry("m-eosc-sb-2024", "Question6");

        assertThat(rows.get(0).get(0)).isEqualTo("FR");
        assertThat(rows.get(0).get(1)).isNull();
    }

    @Test
    void getQuestionAnswersByCountryReturnsOneRowPerValidatedAnswer() {
        SurveyAnswer answer1 = validatedAnswer("sa-1", "sh-1", sectionWithQuestion("Question6", "Yes"));
        SurveyAnswer answer2 = validatedAnswer("sa-2", "sh-2", sectionWithQuestion("Question6", "No"));

        mockSurveyAnswers(answer1, answer2);
        mockStakeholders(stakeholder("sh-1", "GR"), stakeholder("sh-2", "DE"));

        List<List<String>> rows = service.getQuestionAnswersByCountry("m-eosc-sb-2024", "Question6");

        assertThat(rows).containsExactlyInAnyOrder(List.of("GR", "Yes"), List.of("DE", "No"));
    }

    @Test
    void getQuestionAnswersByCountryFetchesStakeholdersOnceRegardlessOfAnswerCount() {
        SurveyAnswer answer1 = validatedAnswer("sa-1", "sh-1", sectionWithQuestion("Question6", "Yes"));
        SurveyAnswer answer2 = validatedAnswer("sa-2", "sh-2", sectionWithQuestion("Question6", "No"));

        mockSurveyAnswers(answer1, answer2);
        mockStakeholders(stakeholder("sh-1", "GR"), stakeholder("sh-2", "DE"));

        service.getQuestionAnswersByCountry("m-eosc-sb-2024", "Question6");

        verify(stakeholderCrudService, times(1)).getAll(any(FacetFilter.class));
        verify(stakeholderCrudService, never()).get(anyString());
    }

    private void mockSurveyAnswers(SurveyAnswer... answers) {
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class)))
                .thenReturn(new Browsing<>(answers.length, 0, answers.length, List.of(answers), List.of()));
    }

    private void mockStakeholders(Stakeholder... stakeholders) {
        when(stakeholderCrudService.getAll(any(FacetFilter.class)))
                .thenReturn(new Browsing<>(stakeholders.length, 0, stakeholders.length, List.of(stakeholders), List.of()));
    }

    private static Stakeholder stakeholder(String id, String country) {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId(id);
        stakeholder.setCountry(country);
        return stakeholder;
    }

    @SuppressWarnings("unchecked")
    private static JSONObject sectionWithQuestion(String questionCode, String value) {
        JSONObject question = new JSONObject();
        question.put(questionCode + "-0", value);
        JSONObject section = new JSONObject();
        section.put(questionCode, question);
        return section;
    }

    @SuppressWarnings("unchecked")
    private static SurveyAnswer validatedAnswer(String id, String stakeholderId, JSONObject section) {
        SurveyAnswer answer = new SurveyAnswer();
        answer.setId(id);
        answer.setStakeholderId(stakeholderId);
        answer.setValidated(true);
        JSONObject root = new JSONObject();
        root.put("section1", section);
        answer.setAnswer(root);
        return answer;
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
