package eu.openaire.observatory.service;

import eu.openaire.observatory.configuration.ApplicationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import eu.openaire.observatory.domain.Administrator;
import eu.openaire.observatory.domain.Coordinator;
import eu.openaire.observatory.domain.History;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.NewsItem;
import eu.openaire.observatory.domain.NotificationPreferences;
import eu.openaire.observatory.domain.Profile;
import eu.openaire.observatory.domain.Settings;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.permissions.PermissionService;
import eu.openaire.observatory.resources.model.Document;
import eu.openaire.observatory.resources.model.DocumentMetadata;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.Version;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
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

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String USER_ID = "user@example.org";

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
    private PrivacyPolicyService privacyPolicyService;
    @Mock
    private CrudService<Stakeholder> stakeholderCrudService;
    @Mock
    private CrudService<Coordinator> coordinatorCrudService;
    @Mock
    private CrudService<Administrator> administratorCrudService;
    @Mock
    private StakeholderService stakeholderService;
    @Mock
    private CoordinatorService coordinatorService;
    @Mock
    private AdministratorService administratorService;
    @Mock
    private CrudService<SurveyAnswer> surveyAnswerCrudService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private SurveyAnswerCommentService commentService;
    @Mock
    private NewsItemService newsItemService;
    @Mock
    private ModelService modelService;
    @Mock
    private GenericResourceService genericResourceService;
    @Mock
    private ErasureSubjectReference erasureSubjectReference;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private ModelResponseValidator validator;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new UserServiceImpl(
                resourceTypeService,
                resourceService,
                searchService,
                versionService,
                parserService,
                privacyPolicyService,
                stakeholderCrudService,
                coordinatorCrudService,
                administratorCrudService,
                stakeholderService,
                coordinatorService,
                administratorService,
                surveyAnswerCrudService,
                permissionService,
                commentService,
                newsItemService,
                modelService,
                genericResourceService,
                erasureSubjectReference,
                applicationProperties,
                validator
        ));

        // Default: no version history, so anonymizeVersions() is a no-op unless a test
        // overrides these with a Resource that actually has Versions on it.
        lenient().when(stakeholderCrudService.getResource(anyString())).thenReturn(new Resource());
        lenient().when(coordinatorCrudService.getResource(anyString())).thenReturn(new Resource());
        lenient().when(administratorCrudService.getResource(anyString())).thenReturn(new Resource());
        lenient().when(surveyAnswerCrudService.getResource(anyString())).thenReturn(new Resource());
        lenient().when(newsItemService.getResource(anyString())).thenReturn(new Resource());
        lenient().doReturn(new Resource()).when(service).getResource(USER_ID);

        // purge() now sweeps EVERY group of each type (full getAll sweep) to scrub the version
        // history of groups the user has left, not just current-membership groups. Default to an
        // empty sweep; tests that assert group version scrubbing override these.
        lenient().when(stakeholderCrudService.getAll(any(FacetFilter.class))).thenReturn(browsing());
        lenient().when(coordinatorCrudService.getAll(any(FacetFilter.class))).thenReturn(browsing());
        lenient().when(administratorCrudService.getAll(any(FacetFilter.class))).thenReturn(browsing());
        lenient().when(newsItemService.getAll(any(FacetFilter.class))).thenReturn(browsing());

        // Untyped sweeps (survey definitions, documents) default to empty too.
        lenient().when(modelService.browse(any(FacetFilter.class))).thenReturn(browsing());
        lenient().when(genericResourceService.getResults(any(FacetFilter.class))).thenReturn(browsing());
    }

    @Test
    void purgeAnonymizesNewsItemAuthorship() throws ResourceNotFoundException {
        NewsItem authored = newsItem("news-1", USER_ID, USER_ID);
        NewsItem unrelated = newsItem("news-2", "someone-else@example.org", "someone-else@example.org");
        when(newsItemService.getAll(any(FacetFilter.class))).thenReturn(browsing(authored, unrelated));
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf());
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        assertEquals(UserServiceImpl.DELETED_USER_PLACEHOLDER, authored.getMetadata().getCreatedBy());
        assertEquals(UserServiceImpl.DELETED_USER_PLACEHOLDER, authored.getMetadata().getModifiedBy());
        verify(newsItemService).saveScrubbed("news-1", authored);

        // Untouched articles are not rewritten, so the sweep stays idempotent.
        assertEquals("someone-else@example.org", unrelated.getMetadata().getCreatedBy());
        verify(newsItemService, never()).saveScrubbed(eq("news-2"), any());
    }

    /**
     * Guard test. NewsItemService#update restores the stored metadata and then re-stamps modifiedBy
     * from the security context, so persisting a scrub through it would replace the purged user's
     * address with the purging admin's. If anyone switches the persist step back to update(), this
     * fails loudly rather than silently trading one person's PII for another's.
     */
    @Test
    void purgeDoesNotPersistNewsItemsThroughUpdate() throws ResourceNotFoundException {
        NewsItem authored = newsItem("news-1", USER_ID, USER_ID);
        when(newsItemService.getAll(any(FacetFilter.class))).thenReturn(browsing(authored));
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf());
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        verify(newsItemService, never()).update(anyString(), any(NewsItem.class));
    }

    @Test
    void purgeAnonymizesSurveyDefinitionAuthorship() throws Exception {
        Model authored = model("model-1", USER_ID, USER_ID);
        when(modelService.browse(any(FacetFilter.class))).thenReturn(browsing(authored));
        stubUntypedResource("model", "model-1");
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf());
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        assertEquals(UserServiceImpl.DELETED_USER_PLACEHOLDER, authored.getCreatedBy());
        assertEquals(UserServiceImpl.DELETED_USER_PLACEHOLDER, authored.getModifiedBy());
        verify(genericResourceService).update("model", "model-1", authored);
    }

    /**
     * Guard test. DefaultModelService#update re-stamps modificationDate, mutates section structure
     * via createSectionIds/createParents, and runs validation that can throw on models dating from
     * 2021-2022 — none of which should happen for an administrative erasure. It is also the join
     * point for the SurveyAspect advice that can email every stakeholder on a deadline change or
     * reopening. If anyone switches the write path back to modelService.update(), this fails.
     */
    @Test
    void purgeDoesNotRestampOrRevalidateSurveyDefinitions() throws Exception {
        Model authored = model("model-1", USER_ID, USER_ID);
        Date originalModificationDate = authored.getModificationDate();
        when(modelService.browse(any(FacetFilter.class))).thenReturn(browsing(authored));
        stubUntypedResource("model", "model-1");
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf());
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        verify(modelService, never()).update(anyString(), any(Model.class));
        assertEquals(originalModificationDate, authored.getModificationDate());
    }

    @Test
    void purgeAnonymizesDocumentMetadataOnly() throws Exception {
        Document authored = document("doc-1", USER_ID, USER_ID);
        JsonNode docInfo = authored.getDocInfo();
        when(genericResourceService.getResults(any(FacetFilter.class))).thenReturn(browsing(authored));
        stubUntypedResource("document", "doc-1");
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf());
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        assertEquals(UserServiceImpl.DELETED_USER_PLACEHOLDER, authored.getMetadata().getCreatedBy());
        assertEquals(UserServiceImpl.DELETED_USER_PLACEHOLDER, authored.getMetadata().getModifiedBy());
        verify(genericResourceService).update("document", "doc-1", authored);

        // Harvested content is third-party bibliographic data, deliberately untouched.
        assertSame(docInfo, authored.getDocInfo());
        assertFalse(authored.isCurated());
    }

    private void stubUntypedResource(String resourceType, String id) {
        Resource located = new Resource();
        located.setId(id);
        doReturn(located).when(service).searchResource(resourceType, id, true);
        lenient().when(resourceService.getResource(id)).thenReturn(new Resource());
    }

    private Model model(String id, String createdBy, String modifiedBy) {
        Model model = new Model();
        model.setId(id);
        model.setCreatedBy(createdBy);
        model.setModifiedBy(modifiedBy);
        model.setModificationDate(new Date(1_000L));
        return model;
    }

    private Document document(String id, String createdBy, String modifiedBy) {
        Document document = new Document();
        document.setId(id);
        document.setDocInfo(JsonNodeFactory.instance.objectNode().put("title", "harvested"));
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setCreatedBy(createdBy);
        metadata.setModifiedBy(modifiedBy);
        document.setMetadata(metadata);
        return document;
    }

    private NewsItem newsItem(String id, String createdBy, String modifiedBy) {
        NewsItem item = new NewsItem();
        item.setId(id);
        Metadata metadata = new Metadata();
        metadata.setCreatedBy(createdBy);
        metadata.setModifiedBy(modifiedBy);
        item.setMetadata(metadata);
        return item;
    }

    @Test
    void purgeRemovesUserFromAllGroupsAnonymizesSurveyAnswersAndDeletesRecord() throws ResourceNotFoundException {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId("sh-1");
        Coordinator coordinator = new Coordinator();
        coordinator.setId("co-1");
        Administrator administrator = new Administrator();
        administrator.setId("ad-1");

        when(stakeholderCrudService.getWithFilter("users", USER_ID)).thenReturn(Set.of(stakeholder));
        when(coordinatorCrudService.getWithFilter("users", USER_ID)).thenReturn(Set.of(coordinator));
        when(administratorCrudService.getWithFilter("users", USER_ID)).thenReturn(Set.of(administrator));

        SurveyAnswer answeredAsEditor = surveyAnswer("sa-1");
        answeredAsEditor.getHistory().addEntry(USER_ID, "role", "comment", new java.util.Date(), History.HistoryAction.UPDATED);
        SurveyAnswer answeredAsCreator = surveyAnswer("sa-2");
        answeredAsCreator.getMetadata().setCreatedBy(USER_ID);
        SurveyAnswer answeredAsModifier = surveyAnswer("sa-3");
        answeredAsModifier.getMetadata().setModifiedBy(USER_ID);
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class)))
                .thenReturn(browsingOf(answeredAsEditor, answeredAsCreator, answeredAsModifier));

        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        verify(stakeholderService).removeMember("sh-1", USER_ID);
        verify(stakeholderService).removeAdmin("sh-1", USER_ID);
        verify(coordinatorService).removeMember("co-1", USER_ID);
        verify(coordinatorService).removeAdmin("co-1", USER_ID);
        verify(administratorService).removeMember("ad-1", USER_ID);

        assertEquals(UserServiceImpl.DELETED_USER_PLACEHOLDER, answeredAsEditor.getHistory().getEntries().getFirst().getEditors().getFirst().getUser());
        assertEquals(UserServiceImpl.DELETED_USER_PLACEHOLDER, answeredAsCreator.getMetadata().getCreatedBy());
        assertEquals(UserServiceImpl.DELETED_USER_PLACEHOLDER, answeredAsModifier.getMetadata().getModifiedBy());
        verify(surveyAnswerCrudService).update("sa-1", answeredAsEditor);
        verify(surveyAnswerCrudService).update("sa-2", answeredAsCreator);
        verify(surveyAnswerCrudService).update("sa-3", answeredAsModifier);

        verify(commentService).anonymizeUser(USER_ID, UserServiceImpl.DELETED_USER_PLACEHOLDER);
        verify(permissionService).removeAll(USER_ID);
        verify(service).delete(USER_ID);
    }

    /**
     * SurveyAnswerRevisionsAggregation#updateHistory writes modifiedBy as a comma-joined list of a
     * session's editors, so the previous whole-string equals never fired against it and the address
     * survived the purge. 199 survey_answer version rows in the dev database carry this shape.
     */
    @Test
    void purgeScrubsUserOutOfCommaJoinedModifiedBy() throws ResourceNotFoundException {
        SurveyAnswer coEdited = surveyAnswer("sa-1");
        coEdited.getMetadata().setModifiedBy("alice@example.org," + USER_ID + ",bob@example.org");
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf(coEdited));
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        assertEquals("alice@example.org," + UserServiceImpl.DELETED_USER_PLACEHOLDER + ",bob@example.org",
                coEdited.getMetadata().getModifiedBy());
        verify(surveyAnswerCrudService).update("sa-1", coEdited);
    }

    /**
     * Placeholders collapse globally rather than only where they repeat consecutively, so purging a
     * second person later does not leave two identical markers. The per-editor rows in
     * history.entries[].editors[] still record how many distinct people edited.
     */
    @Test
    void purgeCollapsesRepeatedPlaceholdersInModifiedBy() throws ResourceNotFoundException {
        SurveyAnswer coEdited = surveyAnswer("sa-1");
        coEdited.getMetadata().setModifiedBy(
                "alice@example.org," + USER_ID + "," + UserServiceImpl.DELETED_USER_PLACEHOLDER);
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf(coEdited));
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        assertEquals("alice@example.org," + UserServiceImpl.DELETED_USER_PLACEHOLDER,
                coEdited.getMetadata().getModifiedBy());
    }

    /**
     * The regression that matters most: re-running purge on an already-scrubbed answer must write
     * nothing, because purge() is re-runnable by design after a partial failure.
     */
    @Test
    void purgeIsIdempotentOnAlreadyScrubbedModifiedBy() throws ResourceNotFoundException {
        SurveyAnswer alreadyScrubbed = surveyAnswer("sa-1");
        alreadyScrubbed.getMetadata().setModifiedBy(
                "alice@example.org," + UserServiceImpl.DELETED_USER_PLACEHOLDER);
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf(alreadyScrubbed));
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        assertEquals("alice@example.org," + UserServiceImpl.DELETED_USER_PLACEHOLDER,
                alreadyScrubbed.getMetadata().getModifiedBy());
        verify(surveyAnswerCrudService, never()).update(eq("sa-1"), any());
    }

    /**
     * The stored separator is "," but the scrub tolerates a spaced variant, and matching is done on
     * the normalized form so casing and quote-wrapping cannot hide an id.
     */
    @Test
    void purgeMatchesNonCanonicalTokensInModifiedBy() throws ResourceNotFoundException {
        SurveyAnswer coEdited = surveyAnswer("sa-1");
        coEdited.getMetadata().setModifiedBy("alice@example.org, \"User@Example.ORG\"");
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf(coEdited));
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        assertEquals("alice@example.org," + UserServiceImpl.DELETED_USER_PLACEHOLDER,
                coEdited.getMetadata().getModifiedBy());
    }

    @Test
    void purgeLeavesSurveyAnswerUntouchedWhenUserNotReferenced() throws ResourceNotFoundException {
        SurveyAnswer unrelated = surveyAnswer("sa-1");
        unrelated.getHistory().addEntry("someone-else@example.org", "role", "comment", new java.util.Date(), History.HistoryAction.UPDATED);
        unrelated.getMetadata().setCreatedBy("someone-else@example.org");
        unrelated.getMetadata().setModifiedBy("someone-else@example.org");
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf(unrelated));
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        verify(surveyAnswerCrudService, never()).update(eq("sa-1"), any());
    }

    @Test
    void purgeNormalizesIdCaseBeforeQuerying() throws ResourceNotFoundException {
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf());
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge("User@Example.ORG");

        verify(stakeholderCrudService).getWithFilter("users", USER_ID);
        verify(coordinatorCrudService).getWithFilter("users", USER_ID);
        verify(administratorCrudService).getWithFilter("users", USER_ID);
        verify(commentService).anonymizeUser(USER_ID, UserServiceImpl.DELETED_USER_PLACEHOLDER);
        verify(permissionService).removeAll(USER_ID);
        verify(service).delete(USER_ID);
    }

    @Test
    void purgeAnonymizesStakeholderVersionHistory() throws ResourceNotFoundException {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setId("sh-1");
        when(stakeholderCrudService.getWithFilter("users", USER_ID)).thenReturn(Set.of(stakeholder));
        // Version scrubbing runs via the full getAll sweep over all groups.
        when(stakeholderCrudService.getAll(any(FacetFilter.class))).thenReturn(browsing(stakeholder));

        Version version = new Version();
        version.setPayload("old-payload");
        version.setResourceTypeName("stakeholder");
        ResourceType resourceType = new ResourceType();
        resourceType.setPayloadType("json");
        version.setResourceType(resourceType);
        Resource stakeholderResource = new Resource();
        stakeholderResource.setVersions(List.of(version));
        when(stakeholderCrudService.getResource("sh-1")).thenReturn(stakeholderResource);

        Stakeholder historicalStakeholder = new Stakeholder();
        historicalStakeholder.setMembers(new TreeSet<>(Set.of(USER_ID, "other@example.org")));
        historicalStakeholder.setAdmins(new TreeSet<>(Set.of(USER_ID)));
        doReturn(Stakeholder.class).when(service).getClassFromResourceType("stakeholder");
        when(parserService.deserialize(stakeholderResource, Stakeholder.class)).thenReturn(historicalStakeholder);
        when(parserService.serialize(eq(historicalStakeholder), any())).thenReturn("new-payload");
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf());

        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        verify(versionService).updateVersion(version);
        assertEquals("new-payload", version.getPayload());
        assertFalse(historicalStakeholder.getMembers().contains(USER_ID));
        assertFalse(historicalStakeholder.getAdmins().contains(USER_ID));
    }

    @Test
    void purgeAnonymizesUsersOwnVersionHistory() throws ResourceNotFoundException {
        Version version = new Version();
        version.setPayload("old-payload");
        version.setResourceTypeName("user");
        ResourceType resourceType = new ResourceType();
        resourceType.setPayloadType("json");
        version.setResourceType(resourceType);
        Resource userResource = new Resource();
        userResource.setVersions(List.of(version));
        doReturn(userResource).when(service).getResource(USER_ID);

        User historicalUser = new User();
        historicalUser.setEmail(USER_ID);
        historicalUser.setSub("sub-123");
        historicalUser.setName("Jane");
        historicalUser.setSurname("Doe");
        historicalUser.setFullname("Jane Doe");
        Profile profile = new Profile();
        profile.setAffiliation("Some University");
        profile.setPosition("Researcher");
        profile.setWebpage("https://jane.example.org");
        historicalUser.setProfile(profile);
        NotificationPreferences prefs = new NotificationPreferences();
        prefs.setForwardEmails(List.of("jane-alt@example.org"));
        Settings settings = new Settings();
        settings.setNotificationPreferences(prefs);
        historicalUser.setSettings(settings);
        doReturn(User.class).when(service).getClassFromResourceType("user");
        when(parserService.deserialize(userResource, User.class)).thenReturn(historicalUser);
        when(parserService.serialize(eq(historicalUser), any())).thenReturn("new-payload");
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf());

        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        verify(versionService).updateVersion(version);
        assertEquals("new-payload", version.getPayload());
        assertNull(historicalUser.getSub());
        assertNull(historicalUser.getEmail());
        assertEquals(UserServiceImpl.DELETED_FIELD_PLACEHOLDER, historicalUser.getName());
        assertEquals(UserServiceImpl.DELETED_FIELD_PLACEHOLDER, historicalUser.getSurname());
        assertEquals(UserServiceImpl.DELETED_USER_PLACEHOLDER, historicalUser.getFullname());
        // The whole profile block is nulled, and forwardEmails cleared, in version history too.
        assertNull(historicalUser.getProfile());
        assertNull(historicalUser.getSettings().getNotificationPreferences().getForwardEmails());
    }

    @Test
    void purgeScrubsVersionHistoryOfGroupTheUserHasLeft() throws ResourceNotFoundException {
        // The user is NOT a current member (getWithFilter returns nothing for this group), but
        // they appear in a historical version. The full getAll sweep must still scrub it.
        Stakeholder leftStakeholder = new Stakeholder();
        leftStakeholder.setId("sh-left");
        when(stakeholderCrudService.getAll(any(FacetFilter.class))).thenReturn(browsing(leftStakeholder));

        Version version = new Version();
        version.setPayload("old-payload");
        version.setResourceTypeName("stakeholder");
        ResourceType resourceType = new ResourceType();
        resourceType.setPayloadType("json");
        version.setResourceType(resourceType);
        Resource resource = new Resource();
        resource.setVersions(List.of(version));
        when(stakeholderCrudService.getResource("sh-left")).thenReturn(resource);

        Stakeholder historical = new Stakeholder();
        historical.setMembers(new TreeSet<>(Set.of(USER_ID)));
        doReturn(Stakeholder.class).when(service).getClassFromResourceType("stakeholder");
        when(parserService.deserialize(resource, Stakeholder.class)).thenReturn(historical);
        when(parserService.serialize(eq(historical), any())).thenReturn("new-payload");
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf());
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        // Not a current member, so there is no live removal for this group...
        verify(stakeholderService, never()).removeMember("sh-left", USER_ID);
        // ...but its historical version is still scrubbed. This is the "left-group" gap fix.
        verify(versionService).updateVersion(version);
        assertFalse(historical.getMembers().contains(USER_ID));
    }

    @Test
    void purgeScrubsAdministratorAdminsFromVersionHistory() throws ResourceNotFoundException {
        Administrator administrator = new Administrator();
        administrator.setId("ad-1");
        when(administratorCrudService.getAll(any(FacetFilter.class))).thenReturn(browsing(administrator));

        Version version = new Version();
        version.setPayload("old-payload");
        version.setResourceTypeName("administrator");
        ResourceType resourceType = new ResourceType();
        resourceType.setPayloadType("json");
        version.setResourceType(resourceType);
        Resource resource = new Resource();
        resource.setVersions(List.of(version));
        when(administratorCrudService.getResource("ad-1")).thenReturn(resource);

        Administrator historical = new Administrator();
        historical.setAdmins(new TreeSet<>(Set.of(USER_ID)));
        doReturn(Administrator.class).when(service).getClassFromResourceType("administrator");
        when(parserService.deserialize(resource, Administrator.class)).thenReturn(historical);
        when(parserService.serialize(eq(historical), any())).thenReturn("new-payload");
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class))).thenReturn(browsingOf());
        doReturn(new User()).when(service).delete(USER_ID);

        service.purge(USER_ID);

        // The Administrator group's admins set is scrubbed in version history too (not just members).
        verify(versionService).updateVersion(version);
        assertFalse(historical.getAdmins().contains(USER_ID));
    }

    private SurveyAnswer surveyAnswer(String id) {
        SurveyAnswer answer = new SurveyAnswer();
        answer.setId(id);
        return answer;
    }

    private Browsing<SurveyAnswer> browsingOf(SurveyAnswer... answers) {
        Browsing<SurveyAnswer> browsing = new Browsing<>();
        browsing.setResults(List.of(answers));
        return browsing;
    }

    @SafeVarargs
    private static <T> Browsing<T> browsing(T... items) {
        Browsing<T> browsing = new Browsing<>();
        browsing.setResults(List.of(items));
        return browsing;
    }
}
