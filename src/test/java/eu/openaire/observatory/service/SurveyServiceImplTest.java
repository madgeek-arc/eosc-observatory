package eu.openaire.observatory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.domain.History;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.SurveyAnswerRevisionsAggregation;
import eu.openaire.observatory.permissions.PermissionService;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    private Authentication oidcAuthentication() {
        OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of(
                        "sub", "sub-1",
                        "email", "user@example.org",
                        "given_name", "User",
                        "family_name", "Example",
                        "name", "User Example"
                )
        );
        DefaultOidcUser principal = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)),
                idToken,
                "email"
        );
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal,
                "token",
                principal.getAuthorities()
        );
    }
}
