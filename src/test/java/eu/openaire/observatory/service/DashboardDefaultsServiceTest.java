package eu.openaire.observatory.service;

import eu.openaire.observatory.widget.DashboardCode;
import eu.openaire.observatory.widget.Widget;
import eu.openaire.observatory.widget.DashboardDefaults;
import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardDefaultsServiceTest {

    private static final String RESOURCE_TYPE = "dashboard_defaults";

    @Mock ResourceTypeService resourceTypeService;
    @Mock ResourceService resourceService;
    @Mock SearchService searchService;
    @Mock VersionService versionService;
    @Mock ParserService parserService;
    @Mock ModelResponseValidator validator;

    private DashboardDefaultsService service;

    @BeforeEach
    void setUp() {
        service = spy(new DashboardDefaultsService(
                resourceTypeService, resourceService, searchService,
                versionService, parserService, validator));
    }

    @Test
    void createId_combinesCodeAndType() {
        DashboardDefaults d = new DashboardDefaults();
        d.setCode(DashboardCode.COUNTRY_PAGES);
        d.setType("country");

        assertEquals("d-country-pages-country", service.createId(d));
    }

    @Test
    void add_rejectsNullCode() {
        DashboardDefaults d = defaultsWith("country", widget("w-1", true));
        d.setCode(null);

        assertThrows(ValidationException.class, () -> service.add(d));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_rejectsBlankType() {
        DashboardDefaults d = defaultsWith(" ", widget("w-1", true));

        assertThrows(ValidationException.class, () -> service.add(d));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_rejectsNullType() {
        DashboardDefaults d = defaultsWith(null, widget("w-1", true));

        assertThrows(ValidationException.class, () -> service.add(d));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_rejectsBlankWidgetId() {
        DashboardDefaults d = defaultsWith("country", widget(null, true), widget("w-1", true));

        assertThrows(ValidationException.class, () -> service.add(d));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_rejectsEmptyWidgetId() {
        DashboardDefaults d = defaultsWith("country", widget("  ", true));

        assertThrows(ValidationException.class, () -> service.add(d));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_rejectsDuplicateWidgetIds() {
        DashboardDefaults d = defaultsWith("country",
                widget("w-1", true),
                widget("w-2", false),
                widget("w-1", false));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.add(d));
        assertTrue(ex.getMessage().contains("w-1"));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_withNullWidgetList_skipsListValidationAndPersists() {
        DashboardDefaults d = new DashboardDefaults();
        d.setCode(DashboardCode.COUNTRY_PAGES);
        d.setType("country");
        setupForAdd();

        DashboardDefaults result = service.add(d);

        assertSame(d, result);
        verify(resourceService).addResource(any());
    }

    @Test
    void add_withValidWidgets_persists() {
        DashboardDefaults d = defaultsWith("country", widget("w-1", true), widget("w-2", false));
        setupForAdd();

        DashboardDefaults result = service.add(d);

        assertSame(d, result);
        verify(resourceService).addResource(any());
    }

    @Test
    void update_rejectsDuplicateWidgetIds() throws ResourceNotFoundException {
        DashboardDefaults d = defaultsWith("country",
                widget("w-1", true),
                widget("w-1", false));
        d.setId("i-country-pages-country");

        assertThrows(ValidationException.class, () -> service.update("i-country-pages-country", d));
        verifyNoInteractions(resourceService);
    }

    @Test
    void update_withValidWidgets_persists() throws ResourceNotFoundException {
        DashboardDefaults d = defaultsWith("country", widget("w-1", true), widget("w-2", false));
        d.setId("i-country-pages-country");
        setupForUpdate("i-country-pages-country");

        DashboardDefaults result = service.update("i-country-pages-country", d);

        assertSame(d, result);
        verify(resourceService).updateResource(any());
    }

    // --- helpers ---

    private void setupForAdd() {
        ResourceType rt = resourceType();
        when(resourceTypeService.getResourceType(RESOURCE_TYPE)).thenReturn(rt);
        // searchFields returning null means not found → add is allowed
        when(searchService.searchFields(eq(RESOURCE_TYPE), any(SearchService.KeyValue[].class))).thenReturn(null);
    }

    private void setupForUpdate(String id) {
        ResourceType rt = resourceType();
        Resource existing = new Resource();
        when(resourceTypeService.getResourceType(RESOURCE_TYPE)).thenReturn(rt);
        when(searchService.searchFields(eq(RESOURCE_TYPE), any(SearchService.KeyValue[].class))).thenReturn(existing);
    }

    private ResourceType resourceType() {
        ResourceType rt = new ResourceType();
        rt.setName(RESOURCE_TYPE);
        rt.setPayloadType("json");
        return rt;
    }

    private DashboardDefaults defaultsWith(String type, Widget... widgets) {
        DashboardDefaults d = new DashboardDefaults();
        d.setCode(DashboardCode.COUNTRY_PAGES);
        d.setType(type);
        d.setWidgets(List.of(widgets));
        return d;
    }

    private Widget widget(String id, boolean visible) {
        Widget w = new Widget();
        w.setId(id);
        w.setVisible(visible);
        return w;
    }
}
