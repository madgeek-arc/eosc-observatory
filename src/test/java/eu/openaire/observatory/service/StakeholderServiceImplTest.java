package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.permissions.Groups;
import eu.openaire.observatory.permissions.PermissionService;
import eu.openaire.observatory.permissions.Permissions;
import gr.uoa.di.madgik.authorization.domain.Permission;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.ParserService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.SearchService;
import gr.uoa.di.madgik.registry.service.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StakeholderServiceImplTest {

    @Mock
    private ResourceTypeService resourceTypeService;
    @Mock
    private ResourceService resourceService;
    @Mock
    private SearchService searchService;
    @Mock
    private VersionService versionService;
    @Mock
    private ParserService parserService;
    @Mock
    private UserService userService;
    @Mock
    private SurveyService surveyService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private ModelResponseValidator validator;

    private StakeholderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new StakeholderServiceImpl(
                resourceTypeService,
                resourceService,
                searchService,
                versionService,
                parserService,
                userService,
                surveyService,
                permissionService,
                validator
        ));
    }

    @Test
    void createIdUsesCountryForCountryStakeholders() {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setType("country");
        stakeholder.setCountry("GR");

        String id = service.createId(stakeholder);

        assertEquals("sh-country-GR", id);
    }

    @Test
    void createIdNormalizesNameForNonCountryStakeholders() {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setType("ai");
        stakeholder.setName("AI Board 2026!");

        String id = service.createId(stakeholder);

        assertEquals("sh-ai-ai.board.2026", id);
    }

    @Test
    void updateContributorsRemovesOldOnesAndAddsPermissions() {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId("sh-1");
        stakeholder.setMembers(Set.of("old@example.org", "keep@example.org"));
        SortedSet<String> newMembers = new TreeSet<>(Set.of("keep@example.org", "new@example.org"));
        List<SurveyAnswer> allAnswers = List.of(answer("sa-1"), answer("sa-2"));
        List<SurveyAnswer> activeAnswers = List.of(answer("sa-2"));

        doReturn(stakeholder).when(service).get("sh-1");
        doReturn(stakeholder).when(service).update("sh-1", stakeholder);
        when(surveyService.getAllByStakeholder("sh-1")).thenReturn(allAnswers);
        when(surveyService.getActive("sh-1")).thenReturn(activeAnswers);
        when(permissionService.addPermissions(anySet(), anyList(), anyList(), eq(Groups.STAKEHOLDER_CONTRIBUTOR.getKey())))
                .thenReturn(Set.of(new Permission()));

        Stakeholder updated = service.updateContributors("sh-1", newMembers);

        assertEquals(newMembers, updated.getMembers());
        verify(permissionService).removePermissions(
                eq(Set.of("old@example.org")),
                eq(List.of(Permissions.READ.getKey(), Permissions.WRITE.getKey())),
                argThat(ids -> ids.size() == 2 && ids.containsAll(List.of("sa-1", "sa-2"))),
                eq(Groups.STAKEHOLDER_CONTRIBUTOR.getKey())
        );
        verify(permissionService, never()).removeAll(anyString(), anyString());
        verify(permissionService, never()).removeAll(any(Collection.class), anyString());
        verify(permissionService).addPermissions(
                eq(newMembers),
                eq(List.of(Permissions.READ.getKey())),
                argThat(ids -> ids.size() == 2 && ids.containsAll(List.of("sa-1", "sa-2"))),
                eq(Groups.STAKEHOLDER_CONTRIBUTOR.getKey())
        );
        verify(permissionService).addPermissions(
                eq(newMembers),
                eq(List.of(Permissions.READ.getKey(), Permissions.WRITE.getKey())),
                eq(List.of("sa-2")),
                eq(Groups.STAKEHOLDER_CONTRIBUTOR.getKey())
        );
    }

    @Test
    void getManagersFiltersUsersMissingNameOrSurname() {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setAdmins(Set.of("valid@example.org", "missing@example.org"));

        User valid = new User();
        valid.setName("Valid");
        valid.setSurname("Manager");
        User missing = new User();
        missing.setName("Missing");

        doReturn(stakeholder).when(service).get("sh-1");
        when(userService.getUser("valid@example.org")).thenReturn(valid);
        when(userService.getUser("missing@example.org")).thenReturn(missing);

        List<?> managers = service.getManagers("sh-1");

        assertEquals(1, managers.size());
    }

    /**
     * A user who was a manager (admin) and is then switched to contributor (member) must end up
     * with contributor permissions only — no leftover manager permissions.
     */
    @Test
    void managerRemovedThenAddedAsContributorHasOnlyContributorPermissions() {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId("sh-1");
        stakeholder.setAdmins(Set.of("user@example.org"));
        stakeholder.setMembers(new HashSet<>());
        List<SurveyAnswer> allAnswers = List.of(answer("sa-1"), answer("sa-2"));
        List<SurveyAnswer> activeAnswers = List.of(answer("sa-2"));

        doReturn(stakeholder).when(service).get("sh-1");
        doReturn(stakeholder).when(service).update(eq("sh-1"), any());
        setupRegistryForUpdate();
        when(surveyService.getAllByStakeholder("sh-1")).thenReturn(allAnswers);
        when(surveyService.getActive("sh-1")).thenReturn(activeAnswers);
        when(permissionService.addPermissions(any(), any(), any(), any())).thenReturn(Set.of());

        service.removeAdmin("sh-1", "user@example.org");
        service.addMember("sh-1", "user@example.org");

        // manager permissions revoked for sh-1's resources
        verify(permissionService).removePermissions(
                argThat(users -> users.size() == 1 && users.contains("user@example.org")),
                argThat(actions -> actions.containsAll(List.of(
                        Permissions.READ.getKey(), Permissions.WRITE.getKey(),
                        Permissions.MANAGE.getKey(), Permissions.PUBLISH.getKey()))),
                argThat(ids -> ids.containsAll(List.of("sa-1", "sa-2")) && ids.size() == 2),
                eq(Groups.STAKEHOLDER_MANAGER.getKey())
        );
        verify(permissionService, never()).removeAll(anyString(), anyString());
        verify(permissionService, never()).removeAll(any(Collection.class), anyString());

        // contributor permissions granted
        verify(permissionService).addPermissions(
                argThat(users -> users.size() == 1 && users.contains("user@example.org")),
                eq(List.of(Permissions.READ.getKey())),
                argThat(ids -> ids.containsAll(List.of("sa-1", "sa-2")) && ids.size() == 2),
                eq(Groups.STAKEHOLDER_CONTRIBUTOR.getKey())
        );
        verify(permissionService).addPermissions(
                argThat(users -> users.size() == 1 && users.contains("user@example.org")),
                eq(List.of(Permissions.READ.getKey(), Permissions.WRITE.getKey())),
                eq(List.of("sa-2")),
                eq(Groups.STAKEHOLDER_CONTRIBUTOR.getKey())
        );
    }

    /**
     * Removing a user from one stakeholder must not touch permissions on a second stakeholder
     * that the same user belongs to.
     */
    @Test
    void removingFromOneStakeholderDoesNotAffectPermissionsOnAnother() {
        Stakeholder stakeholder1 = new Stakeholder();
        stakeholder1.setId("sh-1");
        stakeholder1.setAdmins(Set.of("user@example.org"));
        List<SurveyAnswer> sh1Answers = List.of(answer("sa-1"), answer("sa-2"));

        doReturn(stakeholder1).when(service).get("sh-1");
        setupRegistryForUpdate();
        when(surveyService.getAllByStakeholder("sh-1")).thenReturn(sh1Answers);

        service.removeAdmin("sh-1", "user@example.org");

        // sh-1 resources were revoked
        verify(permissionService).removePermissions(
                argThat(users -> users.size() == 1 && users.contains("user@example.org")),
                argThat(actions -> actions.containsAll(List.of(
                        Permissions.READ.getKey(), Permissions.WRITE.getKey(),
                        Permissions.MANAGE.getKey(), Permissions.PUBLISH.getKey()))),
                argThat(ids -> ids.containsAll(List.of("sa-1", "sa-2")) && ids.size() == 2),
                eq(Groups.STAKEHOLDER_MANAGER.getKey())
        );
        // exactly one removePermissions call — sh-2's resources were never touched
        verify(permissionService, times(1)).removePermissions(any(), any(), any(), any());
        verify(permissionService, never()).removeAll(anyString(), anyString());
        verify(permissionService, never()).removeAll(any(Collection.class), anyString());
        verify(surveyService, never()).getAllByStakeholder("sh-2");
    }

    /**
     * Stubs the minimal registry infrastructure needed when a method internally calls
     * {@code super.update} (via {@code AbstractUserGroupService.removeAdmin / removeMember}),
     * which bypasses Mockito's spy interception and hits {@code AbstractCrudService.update}
     * directly.
     */
    private void setupRegistryForUpdate() {
        ResourceType rt = new ResourceType();
        rt.setPayloadType("json");
        lenient().when(resourceTypeService.getResourceType("stakeholder")).thenReturn(rt);
        lenient().when(searchService.searchFields(anyString(), any(SearchService.KeyValue[].class)))
                .thenReturn(new Resource());
        lenient().when(parserService.serialize(any(), any(ParserService.ParserServiceTypes.class)))
                .thenReturn("{}");
    }

    private SurveyAnswer answer(String id) {
        SurveyAnswer answer = new SurveyAnswer();
        answer.setId(id);
        return answer;
    }
}
