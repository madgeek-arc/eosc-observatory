package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.widget.DashboardCode;
import eu.openaire.observatory.widget.Widget;
import eu.openaire.observatory.widget.DashboardDefaults;
import eu.openaire.observatory.widget.DashboardOverrides;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardOverrideServiceTest {

    private static final String RESOURCE_TYPE = "dashboard_overrides";
    private static final DashboardCode CODE = DashboardCode.COUNTRY_PAGES;
    private static final String TYPE = "country";

    @Mock ResourceTypeService resourceTypeService;
    @Mock ResourceService resourceService;
    @Mock SearchService searchService;
    @Mock VersionService versionService;
    @Mock ParserService parserService;
    @Mock ModelResponseValidator validator;
    @Mock StakeholderService stakeholderService;
    @Mock DashboardDefaultsService defaultsService;

    private DashboardOverrideService service;

    @BeforeEach
    void setUp() {
        service = spy(new DashboardOverrideService(
                stakeholderService, defaultsService,
                resourceTypeService, resourceService, searchService,
                versionService, parserService, validator));
    }

    // --- createId ---

    @Test
    void createId_combinesCodeTypeAndGroupId() {
        DashboardOverrides override = overrideWith("sh-1");

        assertEquals("d-country-pages-country-sh-1", service.createId(override));
    }

    // --- add validation ---

    @Test
    void add_rejectsNullCode() {
        DashboardOverrides override = overrideWith("sh-1", widget("w-1"));
        override.setCode(null);

        assertThrows(ValidationException.class, () -> service.add(override));
        verifyNoInteractions(resourceService, stakeholderService, defaultsService);
    }

    @Test
    void add_rejectsBlankType() {
        DashboardOverrides override = overrideWith("sh-1", widget("w-1"));
        override.setType(" ");

        assertThrows(ValidationException.class, () -> service.add(override));
        verifyNoInteractions(resourceService, stakeholderService, defaultsService);
    }

    @Test
    void add_rejectsBlankWidgetId() {
        DashboardOverrides override = overrideWith("sh-1", widget(null), widget("w-1"));

        assertThrows(ValidationException.class, () -> service.add(override));
        verifyNoInteractions(resourceService, stakeholderService, defaultsService);
    }

    @Test
    void add_rejectsDuplicateWidgetIds() {
        DashboardOverrides override = overrideWith("sh-1",
                widget("w-1"), widget("w-2"), widget("w-1"));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.add(override));
        assertTrue(ex.getMessage().contains("w-1"));
        verifyNoInteractions(resourceService, stakeholderService, defaultsService);
    }

    @Test
    void add_rejectsOverrideWithIdNotPresentInDefaults() {
        DashboardOverrides override = overrideWith("sh-1", widget("w-1"), widget("unknown"));
        when(defaultsService.getByCodeAndType(CODE, TYPE))
                .thenReturn(Optional.of(defaultsWith(TYPE, widget("w-1"))));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.add(override));
        assertTrue(ex.getMessage().contains("unknown"));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_withNoDefaultsConfigured_skipsUnknownIdCheck() {
        DashboardOverrides override = overrideWith("sh-1", widget("any-id"));
        when(defaultsService.getByCodeAndType(CODE, TYPE)).thenReturn(Optional.empty());
        setupForAdd();

        assertDoesNotThrow(() -> service.add(override));
        verify(resourceService).addResource(any());
    }

    @Test
    void add_withValidOverrides_persists() {
        DashboardOverrides override = overrideWith("sh-1", widget("w-1"), widget("w-2"));
        when(defaultsService.getByCodeAndType(CODE, TYPE))
                .thenReturn(Optional.of(defaultsWith(TYPE, widget("w-1"), widget("w-2"), widget("w-3"))));
        setupForAdd();

        DashboardOverrides result = service.add(override);

        assertSame(override, result);
        verify(resourceService).addResource(any());
    }

    @Test
    void add_withNullWidgetList_skipsListValidationAndPersists() {
        DashboardOverrides override = new DashboardOverrides();
        override.setCode(CODE);
        override.setType(TYPE);
        override.setGroupId("sh-1");
        setupForAdd();

        service.add(override);

        verify(resourceService).addResource(any());
        verifyNoInteractions(defaultsService);
    }

    // --- update validation ---

    @Test
    void update_rejectsDuplicateWidgetIds() {
        DashboardOverrides override = overrideWith("sh-1", widget("w-1"), widget("w-1"));
        override.setId("i-country-pages-country-sh-1");

        assertThrows(ValidationException.class, () -> service.update("i-country-pages-country-sh-1", override));
        verifyNoInteractions(resourceService, stakeholderService, defaultsService);
    }

    @Test
    void update_withValidOverrides_persists() throws ResourceNotFoundException {
        DashboardOverrides override = overrideWith("sh-1", widget("w-1"));
        override.setId("i-country-pages-country-sh-1");
        when(defaultsService.getByCodeAndType(CODE, TYPE))
                .thenReturn(Optional.of(defaultsWith(TYPE, widget("w-1"), widget("w-2"))));
        setupForUpdate("i-country-pages-country-sh-1");

        service.update("i-country-pages-country-sh-1", override);

        verify(resourceService).updateResource(any());
    }

    // --- getEffectiveWidgets ---

    @Test
    void getEffectiveWidgets_returnsDefaultsWhenNoOverrideExists() {
        Widget w1 = fullWidget("w-1", "Label 1", true, "number", "Open Data");
        Widget w2 = fullWidget("w-2", "Label 2", false, "percentage", "Open Software");
        when(defaultsService.getByCodeAndType(CODE, TYPE))
                .thenReturn(Optional.of(defaultsWith(TYPE, w1, w2)));
        doReturn(Optional.empty()).when(service).getByCodeAndTypeAndGroupId(CODE, TYPE, "sh-1");

        List<Widget> result = service.getEffectiveWidgets(CODE, TYPE, "sh-1");

        assertEquals(2, result.size());
        assertSame(w1, result.get(0));
        assertSame(w2, result.get(1));
    }

    @Test
    void getEffectiveWidgets_returnsDefaultsWhenOverrideHasNullWidgets() {
        Widget w1 = fullWidget("w-1", "Label 1", true, "number", "Open Data");
        DashboardOverrides override = overrideWith("sh-1");
        when(defaultsService.getByCodeAndType(CODE, TYPE))
                .thenReturn(Optional.of(defaultsWith(TYPE, w1)));
        doReturn(Optional.of(override)).when(service).getByCodeAndTypeAndGroupId(CODE, TYPE, "sh-1");

        List<Widget> result = service.getEffectiveWidgets(CODE, TYPE, "sh-1");

        assertEquals(1, result.size());
        assertSame(w1, result.get(0));
    }

    @Test
    void getEffectiveWidgets_appliesVisibilityOverride() {
        Widget defaultWidget = fullWidget("w-1", "Label 1", true, "number", "Open Data");
        DashboardOverrides override = overrideWith("sh-1", visibilityOverride("w-1", false));
        when(defaultsService.getByCodeAndType(CODE, TYPE))
                .thenReturn(Optional.of(defaultsWith(TYPE, defaultWidget)));
        doReturn(Optional.of(override)).when(service).getByCodeAndTypeAndGroupId(CODE, TYPE, "sh-1");

        List<Widget> result = service.getEffectiveWidgets(CODE, TYPE, "sh-1");

        assertEquals(1, result.size());
        assertEquals("w-1", result.get(0).getId());
        assertFalse(result.get(0).isVisible(), "override should set visible=false");
        assertEquals("Label 1", result.get(0).getLabel(), "label should be preserved from defaults");
        assertEquals("number", result.get(0).getFormat(), "format should be preserved from defaults");
        assertEquals("Open Data", result.get(0).getGroup(), "group should be preserved from defaults");
    }

    @Test
    void getEffectiveWidgets_overrideMakesHiddenWidgetVisible() {
        Widget defaultWidget = fullWidget("w-1", "Label 1", false, "chart", "Open Software");
        DashboardOverrides override = overrideWith("sh-1", visibilityOverride("w-1", true));
        when(defaultsService.getByCodeAndType(CODE, TYPE))
                .thenReturn(Optional.of(defaultsWith(TYPE, defaultWidget)));
        doReturn(Optional.of(override)).when(service).getByCodeAndTypeAndGroupId(CODE, TYPE, "sh-1");

        List<Widget> result = service.getEffectiveWidgets(CODE, TYPE, "sh-1");

        assertTrue(result.get(0).isVisible());
    }

    @Test
    void getEffectiveWidgets_preservesDefaultForWidgetsNotInOverride() {
        Widget w1 = fullWidget("w-1", "Label 1", true, "number", "Open Data");
        Widget w2 = fullWidget("w-2", "Label 2", true, "percentage", "Open Data");
        DashboardOverrides override = overrideWith("sh-1", visibilityOverride("w-1", false));
        when(defaultsService.getByCodeAndType(CODE, TYPE))
                .thenReturn(Optional.of(defaultsWith(TYPE, w1, w2)));
        doReturn(Optional.of(override)).when(service).getByCodeAndTypeAndGroupId(CODE, TYPE, "sh-1");

        List<Widget> result = service.getEffectiveWidgets(CODE, TYPE, "sh-1");

        assertEquals(2, result.size());
        assertFalse(result.get(0).isVisible(), "w-1 overridden to hidden");
        assertSame(w2, result.get(1), "w-2 not overridden — original returned");
    }

    @Test
    void getEffectiveWidgets_overriddenWidgetKeepsEveryDefaultField() {
        Widget defaultWidget = fullWidget("w-1", "Label 1", true, "number", "Open Data");
        DashboardOverrides override = overrideWith("sh-1", visibilityOverride("w-1", false));
        when(defaultsService.getByCodeAndType(CODE, TYPE))
                .thenReturn(Optional.of(defaultsWith(TYPE, defaultWidget)));
        doReturn(Optional.of(override)).when(service).getByCodeAndTypeAndGroupId(CODE, TYPE, "sh-1");

        Widget effective = service.getEffectiveWidgets(CODE, TYPE, "sh-1").get(0);

        assertNotSame(defaultWidget, effective);
        assertTrue(defaultWidget.isVisible(), "default widget must not be mutated");
    }

    // --- requireGroupOfType ---

    @Test
    void requireGroupOfType_acceptsMatchingType() {
        when(stakeholderService.get("sh-1")).thenReturn(stakeholderOfType(TYPE));

        assertDoesNotThrow(() -> service.requireGroupOfType(TYPE, "sh-1"));
    }

    @Test
    void requireGroupOfType_rejectsTypeMismatch() {
        when(stakeholderService.get("sh-1")).thenReturn(stakeholderOfType("eosc-sb"));

        assertThrows(ResourceNotFoundException.class, () -> service.requireGroupOfType(TYPE, "sh-1"));
    }

    @Test
    void requireGroupOfType_rejectsUnknownGroup() {
        when(stakeholderService.get("missing")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> service.requireGroupOfType(TYPE, "missing"));
    }

    @Test
    void upsert_rejectsNullCodeWithValidationException() {
        DashboardOverrides override = overrideWith("sh-1", widget("w-1"));
        override.setCode(null);

        assertThrows(ValidationException.class, () -> service.upsert(override));
        verifyNoInteractions(resourceService, stakeholderService);
    }

    @Test
    void upsert_rejectsTypeMismatchBeforeWriting() {
        when(stakeholderService.get("sh-1")).thenReturn(stakeholderOfType("eosc-sb"));

        assertThrows(ResourceNotFoundException.class,
                () -> service.upsert(overrideWith("sh-1", widget("w-1"))));
        verifyNoInteractions(resourceService);
    }

    // --- helpers ---

    private Stakeholder stakeholderOfType(String type) {
        Stakeholder s = new Stakeholder();
        s.setType(type);
        return s;
    }

    private void setupForAdd() {
        ResourceType rt = resourceType();
        when(resourceTypeService.getResourceType(RESOURCE_TYPE)).thenReturn(rt);
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
        d.setCode(CODE);
        d.setType(type);
        d.setWidgets(List.of(widgets));
        return d;
    }

    private DashboardOverrides overrideWith(String groupId, Widget... widgets) {
        DashboardOverrides o = new DashboardOverrides();
        o.setCode(CODE);
        o.setType(TYPE);
        o.setGroupId(groupId);
        o.setWidgets(List.of(widgets));
        return o;
    }

    private Widget widget(String id) {
        Widget w = new Widget();
        w.setId(id);
        w.setVisible(true);
        return w;
    }

    private Widget visibilityOverride(String id, boolean visible) {
        Widget w = new Widget();
        w.setId(id);
        w.setVisible(visible);
        return w;
    }

    private Widget fullWidget(String id, String label, boolean visible, String format, String group) {
        Widget w = new Widget();
        w.setId(id);
        w.setLabel(label);
        w.setVisible(visible);
        w.setFormat(format);
        w.setGroup(group);
        return w;
    }
}
