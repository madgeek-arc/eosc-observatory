package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.ChartType;
import eu.openaire.observatory.domain.DashboardItem;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.UserDashboard;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.utils.OidcTestUtils;
import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDashboardServiceTest {

    private static final String EMAIL = "user@example.com";

    @Mock IndicatorQueryValidator indicatorQueryValidator;
    @Mock ActorContext actorContext;
    @Mock ResourceTypeService resourceTypeService;
    @Mock ResourceService resourceService;
    @Mock SearchService searchService;
    @Mock VersionService versionService;
    @Mock ParserService parserService;
    @Mock ModelResponseValidator validator;

    private UserDashboardService service;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        service = spy(new UserDashboardService(
                indicatorQueryValidator, actorContext,
                resourceTypeService, resourceService, searchService,
                versionService, parserService, validator));
        authentication = OidcTestUtils.oidcAuthentication(EMAIL);
    }

    // --- createId ---

    @Test
    void createId_returnsResourceOwnId() {
        UserDashboard dashboard = new UserDashboard();
        dashboard.setId(EMAIL);

        assertEquals(EMAIL, service.createId(dashboard));
    }

    // --- getOrCreateForCurrentUser ---

    @Test
    void getOrCreateForCurrentUser_returnsExistingWhenPresent() {
        UserDashboard existing = new UserDashboard();
        existing.setId(EMAIL);
        existing.setItems(List.of(item("i-1")));
        doReturn(existing).when(service).get(EMAIL);

        UserDashboard result = service.getOrCreateForCurrentUser(authentication);

        assertSame(existing, result);
    }

    @Test
    void getOrCreateForCurrentUser_returnsEmptyTransientDashboardWhenMissing() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);

        UserDashboard result = service.getOrCreateForCurrentUser(authentication);

        assertEquals(EMAIL, result.getId());
        assertTrue(result.getItems().isEmpty());
        verify(resourceService, never()).addResource(any());
    }

    // --- getOrCreate (admin/support, by explicit id) ---

    @Test
    void getOrCreate_returnsExistingWhenPresent() {
        UserDashboard existing = new UserDashboard();
        existing.setId(EMAIL);
        doReturn(existing).when(service).get(EMAIL);

        assertSame(existing, service.getOrCreate(EMAIL));
    }

    @Test
    void getOrCreate_normalizesOwnerIdCasing() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);

        UserDashboard result = service.getOrCreate("User@Example.com");

        assertEquals(EMAIL, result.getId());
        verify(service).get(EMAIL);
    }

    // --- save: new dashboard ---

    @Test
    void save_createsNewDashboardWhenNoneExists() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        DashboardItem item = item("i-1");

        service.save(authentication, List.of(item));

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).add(captor.capture());
        UserDashboard saved = captor.getValue();
        assertEquals(EMAIL, saved.getId());
        assertEquals(EMAIL, saved.getMetadata().getCreatedBy());
        assertEquals(1, saved.getItems().size());
    }

    // --- save: existing dashboard ---

    @Test
    void save_updatesExistingDashboardPreservingCreationMetadata() throws Exception {
        UserDashboard existing = new UserDashboard();
        existing.setId(EMAIL);
        Metadata originalMetadata = new Metadata();
        originalMetadata.setCreatedBy(EMAIL);
        originalMetadata.setCreationDate(new Date(0));
        existing.setMetadata(originalMetadata);
        doReturn(existing).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).update(eq(EMAIL), any());

        service.save(authentication, List.of(item("i-1")));

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).update(eq(EMAIL), captor.capture());
        UserDashboard saved = captor.getValue();
        assertEquals(new Date(0), saved.getMetadata().getCreationDate(), "creation metadata must be preserved");
        assertEquals(EMAIL, saved.getMetadata().getModifiedBy());
    }

    // --- save: admin/support on behalf of another user ---

    @Test
    void save_withExplicitOwnerId_targetsThatOwnerNotTheActor() {
        String adminEmail = "admin@example.com";
        Authentication adminAuth = OidcTestUtils.oidcAuthentication(adminEmail);
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());

        service.save(EMAIL, adminAuth, List.of(item("i-1")));

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).add(captor.capture());
        UserDashboard saved = captor.getValue();
        assertEquals(EMAIL, saved.getId(), "dashboard is still owned by the target user");
        assertEquals(adminEmail, saved.getMetadata().getCreatedBy(), "audit trail records who actually wrote it");
    }

    @Test
    void save_withExplicitOwnerId_recordsActorAsModifiedByOnUpdate() throws Exception {
        String adminEmail = "admin@example.com";
        Authentication adminAuth = OidcTestUtils.oidcAuthentication(adminEmail);
        UserDashboard existing = new UserDashboard();
        existing.setId(EMAIL);
        Metadata originalMetadata = new Metadata();
        originalMetadata.setCreatedBy(EMAIL);
        originalMetadata.setCreationDate(new Date(0));
        existing.setMetadata(originalMetadata);
        doReturn(existing).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).update(eq(EMAIL), any());

        service.save(EMAIL, adminAuth, List.of(item("i-1")));

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).update(eq(EMAIL), captor.capture());
        assertEquals(adminEmail, captor.getValue().getMetadata().getModifiedBy());
    }

    // --- validation ---

    @Test
    void save_rejectsNullChartType() {
        DashboardItem item = item("i-1");
        item.setChartType(null);

        assertThrows(ValidationException.class, () -> service.save(authentication, List.of(item)));
        verifyNoInteractions(resourceService);
    }

    @Test
    void save_rejectsNullQuery() {
        DashboardItem item = item("i-1");
        item.setQuery(null);

        assertThrows(ValidationException.class, () -> service.save(authentication, List.of(item)));
        verifyNoInteractions(resourceService);
    }

    @Test
    void save_rejectsDuplicateItemIds() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.save(authentication, List.of(item("dup"), item("dup"))));
        assertTrue(ex.getMessage().contains("dup"));
        verifyNoInteractions(resourceService);
    }

    @Test
    void save_assignsIdsToItemsMissingIds() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        DashboardItem item = item(null);

        service.save(authentication, List.of(item));

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).add(captor.capture());
        String assignedId = captor.getValue().getItems().get(0).getId();
        assertNotNull(assignedId);
        assertFalse(assignedId.isBlank());
    }

    @Test
    void save_validatesEachItemsQueryThroughIndicatorQueryValidator() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        DashboardItem item = item("i-1");

        service.save(authentication, List.of(item));

        verify(indicatorQueryValidator).compile(item.getQuery(), actorContext);
    }

    // --- helpers ---

    private DashboardItem item(String id) {
        DashboardItem item = new DashboardItem();
        item.setId(id);
        item.setChartType(ChartType.BAR);
        item.setQuery(new IndicatorQuery("publications.count", null, null, null, null, null, null, null));
        return item;
    }
}
