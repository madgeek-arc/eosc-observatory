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
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
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
                surveySettingsService
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
