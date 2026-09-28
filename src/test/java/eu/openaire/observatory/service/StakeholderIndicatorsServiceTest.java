package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.DefaultIndicators;
import eu.openaire.observatory.domain.Indicator;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.StakeholderIndicatorsOverride;
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
class StakeholderIndicatorsServiceTest {

    private static final String RESOURCE_TYPE = "stakeholder_indicators";

    @Mock ResourceTypeService resourceTypeService;
    @Mock ResourceService resourceService;
    @Mock SearchService searchService;
    @Mock VersionService versionService;
    @Mock ParserService parserService;
    @Mock ModelResponseValidator validator;
    @Mock StakeholderService stakeholderService;
    @Mock DefaultIndicatorsService defaultIndicatorsService;

    private StakeholderIndicatorsService service;

    @BeforeEach
    void setUp() {
        service = spy(new StakeholderIndicatorsService(
                stakeholderService, defaultIndicatorsService,
                resourceTypeService, resourceService, searchService,
                versionService, parserService, validator));
    }

    // --- createId ---

    @Test
    void createId_combinesStakeholderTypeAndCountry() {
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));

        StakeholderIndicatorsOverride override = new StakeholderIndicatorsOverride();
        override.setStakeholderId("sh-1");

        assertEquals("i-country-GR", service.createId(override));
    }

    // --- add validation ---

    @Test
    void add_rejectsBlankIndicatorId() {
        StakeholderIndicatorsOverride override = overrideWith("sh-1", indicator(null), indicator("ind-1"));

        assertThrows(ValidationException.class, () -> service.add(override));
        verifyNoInteractions(resourceService, stakeholderService, defaultIndicatorsService);
    }

    @Test
    void add_rejectsDuplicateIndicatorIds() {
        StakeholderIndicatorsOverride override = overrideWith("sh-1",
                indicator("ind-1"), indicator("ind-2"), indicator("ind-1"));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.add(override));
        assertTrue(ex.getMessage().contains("ind-1"));
        verifyNoInteractions(resourceService, stakeholderService, defaultIndicatorsService);
    }

    @Test
    void add_rejectsOverrideWithIdNotPresentInDefaults() {
        StakeholderIndicatorsOverride override = overrideWith("sh-1", indicator("ind-1"), indicator("unknown"));
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));
        when(defaultIndicatorsService.getByType("country"))
                .thenReturn(Optional.of(defaultsWith("country", indicator("ind-1"))));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.add(override));
        assertTrue(ex.getMessage().contains("unknown"));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_withNoDefaultsConfigured_skipsUnknownIdCheck() {
        StakeholderIndicatorsOverride override = overrideWith("sh-1", indicator("any-id"));
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));
        when(defaultIndicatorsService.getByType("country")).thenReturn(Optional.empty());
        setupForAdd();

        assertDoesNotThrow(() -> service.add(override));
        verify(resourceService).addResource(any());
    }

    @Test
    void add_withValidOverrides_persists() {
        StakeholderIndicatorsOverride override = overrideWith("sh-1", indicator("ind-1"), indicator("ind-2"));
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));
        when(defaultIndicatorsService.getByType("country"))
                .thenReturn(Optional.of(defaultsWith("country", indicator("ind-1"), indicator("ind-2"), indicator("ind-3"))));
        setupForAdd();

        StakeholderIndicatorsOverride result = service.add(override);

        assertSame(override, result);
        verify(resourceService).addResource(any());
    }

    @Test
    void add_withNullIndicatorList_skipsValidationAndPersists() {
        StakeholderIndicatorsOverride override = new StakeholderIndicatorsOverride();
        override.setStakeholderId("sh-1");
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));
        setupForAdd();

        service.add(override);

        verify(resourceService).addResource(any());
        verifyNoInteractions(defaultIndicatorsService);
    }

    // --- update validation ---

    @Test
    void update_rejectsDuplicateIndicatorIds() {
        StakeholderIndicatorsOverride override = overrideWith("sh-1", indicator("ind-1"), indicator("ind-1"));
        override.setId("i-country-GR");

        assertThrows(ValidationException.class, () -> service.update("i-country-GR", override));
        verifyNoInteractions(resourceService, stakeholderService, defaultIndicatorsService);
    }

    @Test
    void update_withValidOverrides_persists() throws ResourceNotFoundException {
        StakeholderIndicatorsOverride override = overrideWith("sh-1", indicator("ind-1"));
        override.setId("i-country-GR");
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));
        when(defaultIndicatorsService.getByType("country"))
                .thenReturn(Optional.of(defaultsWith("country", indicator("ind-1"), indicator("ind-2"))));
        setupForUpdate("i-country-GR");

        service.update("i-country-GR", override);

        verify(resourceService).updateResource(any());
    }

    // --- getEffectiveIndicators ---

    @Test
    void getEffectiveIndicators_returnsDefaultsWhenNoOverrideExists() {
        Indicator ind1 = fullIndicator("ind-1", "Label 1", true, "number", "Open Data");
        Indicator ind2 = fullIndicator("ind-2", "Label 2", false, "percentage", "Open Software");
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));
        when(defaultIndicatorsService.getByType("country"))
                .thenReturn(Optional.of(defaultsWith("country", ind1, ind2)));
        doReturn(Optional.empty()).when(service).getByStakeholderId("sh-1");

        List<Indicator> result = service.getEffectiveIndicators("sh-1");

        assertEquals(2, result.size());
        assertSame(ind1, result.get(0));
        assertSame(ind2, result.get(1));
    }

    @Test
    void getEffectiveIndicators_returnsDefaultsWhenOverrideHasNullIndicators() {
        Indicator ind1 = fullIndicator("ind-1", "Label 1", true, "number", "Open Data");
        StakeholderIndicatorsOverride override = new StakeholderIndicatorsOverride();
        override.setStakeholderId("sh-1");
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));
        when(defaultIndicatorsService.getByType("country"))
                .thenReturn(Optional.of(defaultsWith("country", ind1)));
        doReturn(Optional.of(override)).when(service).getByStakeholderId("sh-1");

        List<Indicator> result = service.getEffectiveIndicators("sh-1");

        assertEquals(1, result.size());
        assertSame(ind1, result.get(0));
    }

    @Test
    void getEffectiveIndicators_appliesVisibilityOverride() {
        Indicator defaultInd = fullIndicator("ind-1", "Label 1", true, "number", "Open Data");
        StakeholderIndicatorsOverride override = overrideWith("sh-1", visibilityOverride("ind-1", false));
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));
        when(defaultIndicatorsService.getByType("country"))
                .thenReturn(Optional.of(defaultsWith("country", defaultInd)));
        doReturn(Optional.of(override)).when(service).getByStakeholderId("sh-1");

        List<Indicator> result = service.getEffectiveIndicators("sh-1");

        assertEquals(1, result.size());
        assertEquals("ind-1", result.get(0).getId());
        assertFalse(result.get(0).isVisible(), "override should set visible=false");
        assertEquals("Label 1", result.get(0).getLabel(), "label should be preserved from defaults");
        assertEquals("number", result.get(0).getFormat(), "format should be preserved from defaults");
        assertEquals("Open Data", result.get(0).getGroup(), "group should be preserved from defaults");
    }

    @Test
    void getEffectiveIndicators_overrideMakesHiddenIndicatorVisible() {
        Indicator defaultInd = fullIndicator("ind-1", "Label 1", false, "chart", "Open Software");
        StakeholderIndicatorsOverride override = overrideWith("sh-1", visibilityOverride("ind-1", true));
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));
        when(defaultIndicatorsService.getByType("country"))
                .thenReturn(Optional.of(defaultsWith("country", defaultInd)));
        doReturn(Optional.of(override)).when(service).getByStakeholderId("sh-1");

        List<Indicator> result = service.getEffectiveIndicators("sh-1");

        assertTrue(result.get(0).isVisible());
    }

    @Test
    void getEffectiveIndicators_preservesDefaultForIndicatorsNotInOverride() {
        Indicator ind1 = fullIndicator("ind-1", "Label 1", true, "number", "Open Data");
        Indicator ind2 = fullIndicator("ind-2", "Label 2", true, "percentage", "Open Data");
        StakeholderIndicatorsOverride override = overrideWith("sh-1", visibilityOverride("ind-1", false));
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder("country", "GR"));
        when(defaultIndicatorsService.getByType("country"))
                .thenReturn(Optional.of(defaultsWith("country", ind1, ind2)));
        doReturn(Optional.of(override)).when(service).getByStakeholderId("sh-1");

        List<Indicator> result = service.getEffectiveIndicators("sh-1");

        assertEquals(2, result.size());
        assertFalse(result.get(0).isVisible(), "ind-1 overridden to hidden");
        assertSame(ind2, result.get(1), "ind-2 not overridden — original returned");
    }

    // --- helpers ---

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

    private Stakeholder stakeholder(String type, String country) {
        Stakeholder sh = new Stakeholder();
        sh.setType(type);
        sh.setCountry(country);
        return sh;
    }

    private DefaultIndicators defaultsWith(String type, Indicator... indicators) {
        DefaultIndicators d = new DefaultIndicators();
        d.setType(type);
        d.setIndicators(List.of(indicators));
        return d;
    }

    private StakeholderIndicatorsOverride overrideWith(String stakeholderId, Indicator... indicators) {
        StakeholderIndicatorsOverride o = new StakeholderIndicatorsOverride();
        o.setStakeholderId(stakeholderId);
        o.setIndicators(List.of(indicators));
        return o;
    }

    private Indicator indicator(String id) {
        Indicator i = new Indicator();
        i.setId(id);
        i.setVisible(true);
        return i;
    }

    private Indicator visibilityOverride(String id, boolean visible) {
        Indicator i = new Indicator();
        i.setId(id);
        i.setVisible(visible);
        return i;
    }

    private Indicator fullIndicator(String id, String label, boolean visible, String format, String group) {
        Indicator i = new Indicator();
        i.setId(id);
        i.setLabel(label);
        i.setVisible(visible);
        i.setFormat(format);
        i.setGroup(group);
        return i;
    }
}
