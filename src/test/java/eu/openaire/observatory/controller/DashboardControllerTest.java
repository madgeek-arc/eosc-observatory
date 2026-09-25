package eu.openaire.observatory.controller;

import eu.openaire.observatory.service.DashboardDefaultsService;
import eu.openaire.observatory.service.DashboardOverrideService;
import eu.openaire.observatory.widget.DashboardCode;
import eu.openaire.observatory.widget.DashboardDefaults;
import eu.openaire.observatory.widget.DashboardOverrides;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DashboardControllerTest {

    private static final String CODE = "country-pages";

    private DashboardDefaultsService defaultsService;
    private DashboardOverrideService overrideService;
    private DashboardController controller;

    @BeforeEach
    void setUp() {
        defaultsService = mock(DashboardDefaultsService.class);
        overrideService = mock(DashboardOverrideService.class);
        controller = new DashboardController(defaultsService, overrideService);
    }

    @Test
    void getDashboardCodes_listsWireValues() {
        assertEquals(List.of(CODE), controller.getDashboardCodes().getBody());
    }

    @Test
    void unknownCode_isNotFound() {
        ResourceException ex = assertThrows(ResourceException.class,
                () -> controller.getDefaults("typo-pages", "country"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verifyNoInteractions(defaultsService);
    }

    @Test
    void updateDefaults_forcesPathCodeAndType() throws ResourceNotFoundException {
        DashboardDefaults body = new DashboardDefaults();
        body.setType("other-type");

        controller.updateDefaults(CODE, "country", body);

        assertEquals("country", body.getType());
        assertEquals(DashboardCode.COUNTRY_PAGES, body.getCode());
        verify(defaultsService).upsertByCodeAndType(DashboardCode.COUNTRY_PAGES, "country", body);
    }

    @Test
    void upsertOverrides_forcesPathCodeTypeAndGroup() throws ResourceNotFoundException {
        DashboardOverrides body = new DashboardOverrides();

        controller.upsertOverrides(CODE, "country", "sh-1", body);

        assertEquals(DashboardCode.COUNTRY_PAGES, body.getCode());
        assertEquals("country", body.getType());
        assertEquals("sh-1", body.getGroupId());
        verify(overrideService).upsert(body);
    }

    @Test
    void deleteOverrides_checksGroupTypeBeforeDeleting() throws ResourceNotFoundException {
        controller.deleteOverrides(CODE, "country", "sh-1");

        InOrder order = inOrder(overrideService);
        order.verify(overrideService).requireGroupOfType("country", "sh-1");
        order.verify(overrideService).deleteByCodeAndTypeAndGroupId(DashboardCode.COUNTRY_PAGES, "country", "sh-1");
    }

    @Test
    void deleteOverrides_typeMismatchDoesNotDelete() throws ResourceNotFoundException {
        doThrow(new ResourceNotFoundException("groupId", "sh-1"))
                .when(overrideService).requireGroupOfType("country", "sh-1");

        assertThrows(ResourceNotFoundException.class, () -> controller.deleteOverrides(CODE, "country", "sh-1"));
        verify(overrideService, never()).deleteByCodeAndTypeAndGroupId(any(), any(), any());
    }

    @Test
    void getEffectiveWidgets_checksGroupType() throws ResourceNotFoundException {
        controller.getEffectiveWidgets(CODE, "country", "sh-1");

        verify(overrideService).requireGroupOfType("country", "sh-1");
        verify(overrideService).getEffectiveWidgets(DashboardCode.COUNTRY_PAGES, "country", "sh-1");
    }
}
