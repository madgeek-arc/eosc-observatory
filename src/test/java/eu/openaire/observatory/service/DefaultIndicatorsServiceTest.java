package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.DefaultIndicators;
import eu.openaire.observatory.domain.Indicator;
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
class DefaultIndicatorsServiceTest {

    private static final String RESOURCE_TYPE = "default_indicators";

    @Mock ResourceTypeService resourceTypeService;
    @Mock ResourceService resourceService;
    @Mock SearchService searchService;
    @Mock VersionService versionService;
    @Mock ParserService parserService;
    @Mock ModelResponseValidator validator;

    private DefaultIndicatorsService service;

    @BeforeEach
    void setUp() {
        service = spy(new DefaultIndicatorsService(
                resourceTypeService, resourceService, searchService,
                versionService, parserService, validator));
    }

    @Test
    void createId_usesType() {
        DefaultIndicators d = new DefaultIndicators();
        d.setType("country");

        assertEquals("i-country", service.createId(d));
    }

    @Test
    void add_rejectsBlankIndicatorId() {
        DefaultIndicators d = defaultsWith("country", indicator(null, true), indicator("ind-1", true));

        assertThrows(ValidationException.class, () -> service.add(d));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_rejectsEmptyIndicatorId() {
        DefaultIndicators d = defaultsWith("country", indicator("  ", true));

        assertThrows(ValidationException.class, () -> service.add(d));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_rejectsDuplicateIndicatorIds() {
        DefaultIndicators d = defaultsWith("country",
                indicator("ind-1", true),
                indicator("ind-2", false),
                indicator("ind-1", false));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.add(d));
        assertTrue(ex.getMessage().contains("ind-1"));
        verifyNoInteractions(resourceService);
    }

    @Test
    void add_withNullIndicatorList_skipsValidationAndPersists() {
        DefaultIndicators d = new DefaultIndicators();
        d.setType("country");
        setupForAdd();

        DefaultIndicators result = service.add(d);

        assertSame(d, result);
        verify(resourceService).addResource(any());
    }

    @Test
    void add_withValidIndicators_persists() {
        DefaultIndicators d = defaultsWith("country", indicator("ind-1", true), indicator("ind-2", false));
        setupForAdd();

        DefaultIndicators result = service.add(d);

        assertSame(d, result);
        verify(resourceService).addResource(any());
    }

    @Test
    void update_rejectsDuplicateIndicatorIds() throws ResourceNotFoundException {
        DefaultIndicators d = defaultsWith("country",
                indicator("ind-1", true),
                indicator("ind-1", false));
        d.setId("i-country");

        assertThrows(ValidationException.class, () -> service.update("i-country", d));
        verifyNoInteractions(resourceService);
    }

    @Test
    void update_withValidIndicators_persists() throws ResourceNotFoundException {
        DefaultIndicators d = defaultsWith("country", indicator("ind-1", true), indicator("ind-2", false));
        d.setId("i-country");
        setupForUpdate("i-country");

        DefaultIndicators result = service.update("i-country", d);

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

    private DefaultIndicators defaultsWith(String type, Indicator... indicators) {
        DefaultIndicators d = new DefaultIndicators();
        d.setType(type);
        d.setIndicators(List.of(indicators));
        return d;
    }

    private Indicator indicator(String id, boolean visible) {
        Indicator i = new Indicator();
        i.setId(id);
        i.setVisible(visible);
        return i;
    }
}
