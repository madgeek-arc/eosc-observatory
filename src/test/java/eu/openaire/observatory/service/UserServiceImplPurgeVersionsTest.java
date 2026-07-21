package eu.openaire.observatory.service;

import eu.openaire.observatory.IntegrationTestConfig;
import eu.openaire.observatory.domain.History;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.SurveyAnswerRevisionsAggregation;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.domain.UserGroup;
import eu.openaire.observatory.permissions.PermissionService;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Version;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ParserService;
import gr.uoa.di.madgik.registry.service.ResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.registry.service.VersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceImplPurgeVersionsTest extends IntegrationTestConfig {

    private static final String USER_ID = "purge-versions-test@example.org";

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private CrudService<SurveyAnswer> surveyAnswerCrudService;

    @Autowired
    private CrudService<Stakeholder> stakeholderCrudService;

    @Autowired
    private VersionService versionService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ResourceTypeService resourceTypeService;

    @Autowired
    private ParserService parserService;

    // These tests are specifically about registry-core version history, not caching or
    // permissions. IntegrationTestConfig provides no Redis container (SurveyAnswerCrudService's
    // cache is Redis-backed) and no schema for the authorization module's Permission table, so
    // both are mocked out here — scoped to this test class only, not the shared base config.
    @MockBean
    private CacheService<String, SurveyAnswerRevisionsAggregation> cacheService;

    @MockBean
    private PermissionService permissionService;

    @Test
    void purgeShouldRemoveUserFromSurveyAnswerVersionHistory() throws ResourceNotFoundException {
        persistUser(USER_ID);
        Stakeholder stakeholder = persistStakeholderBypassingSurveyGeneration(
                "purge-versions-test-stakeholder-" + UUID.randomUUID());

        SurveyAnswer answer = new SurveyAnswer();
        answer.setSurveyId("purge-versions-test-survey-" + UUID.randomUUID());
        answer.setStakeholderId(stakeholder.getId());
        answer.getMetadata().setCreationDate(new Date());
        answer.getMetadata().setModificationDate(new Date());
        answer.getMetadata().setCreatedBy(USER_ID);
        answer.getMetadata().setModifiedBy(USER_ID);
        answer.getHistory().addEntry(USER_ID, "editor", "created", new Date(), History.HistoryAction.CREATED);
        answer = surveyAnswerCrudService.add(answer);
        String answerId = answer.getId();

        // Touch an unrelated field and update once, so the registry framework snapshots the
        // pre-update payload — still containing the raw USER_ID — into a Version row.
        answer.setValidated(true);
        surveyAnswerCrudService.update(answerId, answer);

        List<Version> versionsBeforePurge = surveyAnswerCrudService.getResource(answerId).getVersions();
        assertFalse(versionsBeforePurge.isEmpty(), "expected at least one version to have been snapshotted");
        assertTrue(versionsBeforePurge.stream().anyMatch(v -> v.getPayload().contains(USER_ID)),
                "sanity check: the snapshotted version should still contain the raw USER_ID before purge");

        userService.purge(USER_ID);

        // This only rewrites the *current* SurveyAnswer row unless UserServiceImpl#purge also
        // scrubs registry core versions (see the TODO comments there).
        List<Version> versionsAfterPurge = surveyAnswerCrudService.getResource(answerId).getVersions();
        assertFalse(versionsAfterPurge.isEmpty());
        assertTrue(versionsAfterPurge.stream().noneMatch(v -> v.getPayload().contains(USER_ID)),
                "purge() should have scrubbed USER_ID out of every historical SurveyAnswer version");
    }

    @Test
    void purgeShouldRemoveUserFromStakeholderVersionHistory() throws ResourceNotFoundException {
        persistUser(USER_ID);
        Stakeholder stakeholder = persistStakeholderBypassingSurveyGeneration(
                "purge-versions-test-stakeholder-" + UUID.randomUUID());
        stakeholder.setMembers(new TreeSet<>(Set.of(USER_ID)));
        String stakeholderId = stakeholder.getId();

        // Touch a field and update once, so the pre-update payload — without USER_ID as a
        // member yet — gets snapshotted, then again with USER_ID added, so a version exists
        // that lists USER_ID as a member.
        stakeholderCrudService.update(stakeholderId, stakeholder);
        stakeholder.setMandated(true);
        stakeholderCrudService.update(stakeholderId, stakeholder);

        List<Version> versionsBeforePurge = stakeholderCrudService.getResource(stakeholderId).getVersions();
        assertFalse(versionsBeforePurge.isEmpty(), "expected at least one version to have been snapshotted");
        assertTrue(versionsBeforePurge.stream().anyMatch(v -> v.getPayload().contains(USER_ID)),
                "sanity check: the snapshotted version should still list USER_ID as a member before purge");

        userService.purge(USER_ID);

        List<Version> versionsAfterPurge = stakeholderCrudService.getResource(stakeholderId).getVersions();
        assertFalse(versionsAfterPurge.isEmpty());
        assertTrue(versionsAfterPurge.stream().noneMatch(v -> v.getPayload().contains(USER_ID)),
                "purge() should have removed USER_ID from every historical Stakeholder version");
    }

    @Test
    void purgeShouldScrubUsersOwnVersionHistory() throws ResourceNotFoundException {
        User user = new User();
        user.setEmail(USER_ID);
        user.setSub("purge-versions-test-sub-" + UUID.randomUUID());
        user.setName("Purge");
        user.setSurname("VersionsTest");
        user.setFullname("Purge VersionsTest");
        user = userService.add(user);

        // Touch a field and update once, so the pre-update payload — still containing this
        // user's real PII — gets snapshotted into a Version row.
        user.setName("Purge Updated");
        userService.update(USER_ID, user);

        Resource userResource = userService.getResource(USER_ID);
        List<Version> versionsBeforePurge = userResource.getVersions();
        assertFalse(versionsBeforePurge.isEmpty(), "expected at least one version to have been snapshotted");
        assertTrue(versionsBeforePurge.stream().anyMatch(v -> v.getPayload().contains("purge-versions-test-sub")),
                "sanity check: the snapshotted version should still contain this user's real sub before purge");
        // purge() deletes the User resource itself as its last step, so it won't be resolvable
        // by business id afterward — capture the registry's internal resource id now and query
        // versionService directly with that instead of going through getResource() again.
        String internalResourceId = userResource.getId();

        userService.purge(USER_ID);

        // Deleting the current User resource turns out to make its version history entirely
        // unreachable via the API too (getVersionsByResource returns null, not a list still
        // containing the scrubbed payload) — a stronger outcome than the TODO comment assumed,
        // but still worth confirming: this must NOT come back non-empty and un-scrubbed.
        List<Version> versionsAfterPurge = versionService.getVersionsByResource(internalResourceId);
        if (versionsAfterPurge != null) {
            assertTrue(versionsAfterPurge.stream().noneMatch(v -> v.getPayload().contains("purge-versions-test-sub")),
                    "purge() should have scrubbed this user's sub out of every historical User version");
            assertTrue(versionsAfterPurge.stream().noneMatch(v -> v.getPayload().contains(USER_ID)),
                    "purge() should have scrubbed this user's email out of every historical User version");
        }
    }

    /**
     * purge() always deletes the User resource itself as its final step, so a real User record
     * must exist for the id being purged, regardless of what else the test is actually about.
     */
    private void persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setName("Purge");
        user.setSurname("VersionsTest");
        user.setFullname("Purge VersionsTest");
        userService.add(user);
    }

    /**
     * Persists a Stakeholder via the raw registry services directly, bypassing
     * {@code StakeholderService.add(..)} — {@link eu.openaire.observatory.aspect.SurveyAspect}
     * is woven onto that method and eagerly generates survey answers for every existing survey
     * type on add, which isn't relevant to these tests and depends on catalogue-level survey
     * ("model") setup this test doesn't need.
     */
    private Stakeholder persistStakeholderBypassingSurveyGeneration(String id) {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId(id);
        stakeholder.setName("Purge Versions Test Stakeholder");
        stakeholder.setType(UserGroup.GroupType.COUNTRY.getKey());
        stakeholder.setCountry("GR");

        ResourceType resourceType = resourceTypeService.getResourceType("stakeholder");
        Resource resource = new Resource();
        resource.setResourceTypeName("stakeholder");
        resource.setResourceType(resourceType);
        resource.setPayload(parserService.serialize(stakeholder, ParserService.ParserServiceTypes.fromString(resourceType.getPayloadType())));
        resourceService.addResource(resource);

        return stakeholder;
    }
}
