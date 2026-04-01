package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.permissions.Groups;
import eu.openaire.observatory.permissions.PermissionService;
import eu.openaire.observatory.permissions.Permissions;
import gr.uoa.di.madgik.authorization.domain.Permission;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
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

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
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
        verify(permissionService).removeAll(Set.of("old@example.org"), Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
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

    private SurveyAnswer answer(String id) {
        SurveyAnswer answer = new SurveyAnswer();
        answer.setId(id);
        return answer;
    }
}
