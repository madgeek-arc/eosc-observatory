package eu.openaire.observatory.controller;

import eu.openaire.observatory.domain.Administrator;
import eu.openaire.observatory.domain.Coordinator;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.service.AdministratorService;
import eu.openaire.observatory.service.CoordinatorService;
import eu.openaire.observatory.service.CrudService;
import eu.openaire.observatory.service.StakeholderService;
import eu.openaire.observatory.service.SurveyService;
import gr.uoa.di.madgik.catalogue.controller.FormsController;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SurveyControllerTest {

    private SurveyService surveyService;
    private StakeholderService stakeholderService;
    private CoordinatorService coordinatorService;
    private AdministratorService administratorService;
    private SurveyController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        surveyService = mock(SurveyService.class);
        stakeholderService = mock(StakeholderService.class);
        coordinatorService = mock(CoordinatorService.class);
        administratorService = mock(AdministratorService.class);
        controller = new SurveyController(mock(FormsController.class), mock(CrudService.class), surveyService,
                stakeholderService, coordinatorService, administratorService);
    }

    @Test
    void getSurveyInfo_blankGroupId_isBadRequest() {
        ResourceException ex = assertThrows(ResourceException.class, () -> controller.getSurveyInfo(" ", params(" ")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("groupId is missing", ex.getMessage());
        verifyNoInteractions(surveyService);
    }

    @Test
    void getSurveyInfo_unknownPrefix_isNotFound() {
        assertNoSuchGroup("xyz-1");
    }

    @Test
    void getSurveyInfo_missingGroups_areNotFound() {
        when(stakeholderService.get("sh-1")).thenThrow(new ResourceNotFoundException("missing"));
        when(coordinatorService.get("co-1")).thenThrow(new ResourceNotFoundException("missing"));
        when(administratorService.get("admin-1")).thenThrow(new ResourceNotFoundException("missing"));

        assertNoSuchGroup("sh-1");
        assertNoSuchGroup("co-1");
        assertNoSuchGroup("admin-1");
    }

    @Test
    void getSurveyInfo_existingGroup_filtersByItsType() {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setType("eosc-sb");
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder);
        Coordinator coordinator = new Coordinator();
        coordinator.setType("country");
        when(coordinatorService.get("co-1")).thenReturn(coordinator);
        Administrator administrator = new Administrator();
        administrator.setType("eosc-sb");
        when(administratorService.get("admin-1")).thenReturn(administrator);

        assertEquals("eosc-sb", requestedType("sh-1"));
        assertEquals("country", requestedType("co-1"));
        assertEquals("eosc-sb", requestedType("admin-1"));
    }

    @Test
    void getSurveyInfo_groupWithoutType_isServerError() {
        when(administratorService.get("admin-1")).thenReturn(new Administrator());

        ResourceException ex = assertThrows(ResourceException.class, () -> controller.getSurveyInfo("admin-1", params("admin-1")));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertEquals("group has no type", ex.getMessage());
        verifyNoInteractions(surveyService);
    }

    @Test
    void getSurveyInfo_groupIdIsNotPassedAsFilter() {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setType("eosc-sb");
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder);

        controller.getSurveyInfo("sh-1", params("sh-1"));

        ArgumentCaptor<FacetFilter> filter = ArgumentCaptor.forClass(FacetFilter.class);
        verify(surveyService).browseSurveyAnswersInfo(filter.capture());
        assertFalse(filter.getValue().getFilter().containsKey("groupId"));
    }

    private void assertNoSuchGroup(String groupId) {
        ResourceException ex = assertThrows(ResourceException.class, () -> controller.getSurveyInfo(groupId, params(groupId)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("no such groupId", ex.getMessage());
        verifyNoInteractions(surveyService);
    }

    private Object requestedType(String groupId) {
        clearInvocations(surveyService);
        controller.getSurveyInfo(groupId, params(groupId));

        ArgumentCaptor<FacetFilter> filter = ArgumentCaptor.forClass(FacetFilter.class);
        verify(surveyService).browseSurveyAnswersInfo(filter.capture());
        return filter.getValue().getFilter().get("type");
    }

    private static MultiValueMap<String, Object> params(String groupId) {
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("groupId", groupId);
        return params;
    }
}
