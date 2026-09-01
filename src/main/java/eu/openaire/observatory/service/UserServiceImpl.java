/*
 * Copyright 2021-2026 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.openaire.observatory.service;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.domain.*;
import eu.openaire.observatory.permissions.PermissionService;
import eu.openaire.observatory.resources.model.Document;
import eu.openaire.observatory.utils.UserIds;
import gr.athenarc.messaging.service.MessagingService;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.Version;
import gr.uoa.di.madgik.registry.service.*;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Service
public class UserServiceImpl extends AbstractCrudService<User> implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    public static final String DELETED_USER_PLACEHOLDER = "[Deleted User]";
    // For decomposed name fields (User#name/#surname) rather than a whole identity string:
    // some call sites (e.g. UserDTO, StakeholderServiceImpl#getManagers) filter on non-null
    // name/surname or concatenate them, so both must stay non-null and non-duplicated.
    public static final String DELETED_FIELD_PLACEHOLDER = "[Deleted]";
    // Unlike every other resource type, "document" has no shared RESOURCE_TYPE constant — it is a
    // string literal at nine call sites across ResourcesService, ResourcesController and
    // SurveyAnswerDocumentAnalyzer. Kept local here rather than introducing a shared one.
    private static final String DOCUMENT_RESOURCE_TYPE = "document";

    /** Bounds the blocking wait on the messaging service, which sweeps every thread a user appears in. */
    private static final Duration MESSAGING_ERASURE_TIMEOUT = Duration.ofSeconds(30);

    private final PrivacyPolicyService privacyPolicyService;
    private final CrudService<Stakeholder> stakeholderCrudService;
    private final CrudService<Coordinator> coordinatorCrudService;
    private final CrudService<Administrator> administratorCrudService;
    private final StakeholderService stakeholderService;
    private final CoordinatorService coordinatorService;
    private final AdministratorService administratorService;
    private final CrudService<SurveyAnswer> surveyAnswerCrudService;
    private final PermissionService permissionService;
    private final SurveyAnswerCommentService commentService;
    private final NewsItemService newsItemService;
    private final ModelService modelService;
    private final GenericResourceService genericResourceService;
    private final ErasureSubjectReference erasureSubjectReference;
    // Named 'messagingClient' rather than 'messagingService' to keep it distinct from
    // eu.openaire.observatory.messaging.MessagingService, a different class entirely.
    private final MessagingService messagingClient;
    private final ApplicationProperties applicationProperties;

    protected UserServiceImpl(ResourceTypeService resourceTypeService,
                              ResourceService resourceService,
                              SearchService searchService,
                              VersionService versionService,
                              ParserService parserService,
                              PrivacyPolicyService privacyPolicyService,
                              @Lazy CrudService<Stakeholder> stakeholderCrudService,
                              @Lazy CrudService<Coordinator> coordinatorCrudService,
                              @Lazy CrudService<Administrator> administratorCrudService,
                              @Lazy StakeholderService stakeholderService,
                              @Lazy CoordinatorService coordinatorService,
                              @Lazy AdministratorService administratorService,
                              @Lazy CrudService<SurveyAnswer> surveyAnswerCrudService,
                              PermissionService permissionService,
                              @Lazy SurveyAnswerCommentService commentService,
                              @Lazy NewsItemService newsItemService,
                              @Lazy ModelService modelService,
                              @Lazy GenericResourceService genericResourceService,
                              ErasureSubjectReference erasureSubjectReference,
                              MessagingService messagingClient,
                              ApplicationProperties applicationProperties,
                              ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.privacyPolicyService = privacyPolicyService;
        this.stakeholderCrudService = stakeholderCrudService;
        this.coordinatorCrudService = coordinatorCrudService;
        this.administratorCrudService = administratorCrudService;
        this.stakeholderService = stakeholderService;
        this.coordinatorService = coordinatorService;
        this.administratorService = administratorService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.permissionService = permissionService;
        this.commentService = commentService;
        this.newsItemService = newsItemService;
        this.modelService = modelService;
        this.genericResourceService = genericResourceService;
        this.erasureSubjectReference = erasureSubjectReference;
        this.messagingClient = messagingClient;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String createId(User resource) {
        return resource.getEmail();
    }

    @Override
    public String getResourceType() {
        return "user";
    }

    @Override
    public User getUser(String id) {
        User user = null;
        try {
            user = get(id);
        } catch (ResourceNotFoundException e) {
            logger.debug("User not found in DB");
            user = new User();
            user.setEmail(id);
        }
        return user;
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        User user = getUser(userId);
        return createUserInfo(user);
    }

    @Override
    public UserInfo getUserInfo(Authentication authentication) {
        User user;
        try {
            user = get(User.getId(authentication));
        } catch (ResourceNotFoundException e) {
            logger.debug("User not found in DB");
            user = User.of(authentication);
        }
        return createUserInfo(user);
    }

    @Override
    public User acceptPrivacyPolicy(String policyId, Authentication authentication) {
        User user = User.of(authentication);
        PrivacyPolicy policy = privacyPolicyService.get(policyId);

        FacetFilter filter = new FacetFilter();
        filter.addFilter("policyId", policy.getId());
        filter.addFilter("resource_internal_id", User.of(authentication).getId());
        Browsing<User> userBrowsing = getAll(filter);
        if (userBrowsing.getTotal() == 1) {
            //
            user = userBrowsing.getResults().get(0);

        } else if (userBrowsing.getTotal() > 1) {
            logger.error(String.format("More than one user with [id=%s] was found.", user.getId()));
            user = null;
        } else {
            //
            user = get(user.getId());
            if (user.getPoliciesAccepted() == null) {
                user.setPoliciesAccepted(new ArrayList<>());
            }
            user.getPoliciesAccepted().add(new PolicyAccepted(policy.getId(), new Date().getTime()));
            user = update(user.getId(), user);
        }

        return user;
    }

    @Override
    public void updateUserDetails(Authentication authentication) {
        User user = User.of(authentication);
        try {
            User existing = this.get(user.getId());
            if (!existing.getFullname().equals(user.getFullname())) {
                existing.setName(user.getName());
                existing.setSurname(user.getSurname());
                existing.setFullname(user.getFullname());
                this.update(existing.getId(), existing); // save previous user object with updated name/surname
            }
            if (!existing.getSub().equals(user.getSub())) {
                logger.warn("User sub has been changed: '{}' -> '{}'", existing.getSub(), user.getSub());
                existing.setSub(user.getSub());
                this.update(existing.getId(), existing); // save previous user object with updated sub
            }
        } catch (ResourceNotFoundException e) {
            logger.debug(String.format("User not found! Adding User to database [user=%s]", user));
            this.add(user);
        }
    }

    /**
     * Not wrapped in a single transaction: the steps span the registry's
     * Postgres+Elasticsearch-backed group/permission storage, this service's own
     * JPA-backed comment/user storage, and a remote MongoDB-backed messaging service reached
     * over HTTP, so no single transaction manager could cover all of it.
     * Instead, every step here is idempotent (group/permission removal and the placeholder
     * rewrites are no-ops when reapplied), so on partial failure it is safe to simply call
     * purge() again — it will pick up wherever it left off. The one exception is the final
     * delete(id): if a prior run already completed, retrying throws ResourceNotFoundException,
     * which is the expected/idiomatic response for deleting an already-deleted resource.
     */
    @Override
    public void purge(String id) throws ResourceNotFoundException {
        // Normalize up front: ids are stored in canonical form everywhere (see UserIds#normalize,
        // applied on every write path), but this id comes from a path variable and isn't guaranteed
        // to match. Every comparison/query below is an exact match against the stored form.
        id = UserIds.normalize(id);
        // Captured by the anonymizeVersions(...) lambdas below, which need an effectively-final reference.
        final String userId = id;

        // Resolved up front, before anything is mutated. The report line at the end of this method is
        // the only record that the erasure happened, so a misconfigured secret has to fail here —
        // not two thirds of the way through, after the scrubs have run but before delete(id).
        String subjectRef = erasureSubjectReference.of(userId);

        // TODO: scrub the Redis edit-session cache here (SurveyAnswerCrudService#autoSaveCache flushes a cached aggregate back over the erased record) — blocked because deserializing SurveyAnswerRevisionsAggregation invokes its single-arg constructor, which appends a HistoryEntry, so a fetch-mutate-save scrub would corrupt history; see RevisionsCacheRoundTripTest.

        // --- Live removal from the groups the user is CURRENTLY a member/admin of ---
        // removeMember/removeAdmin handle permission cleanup internally. Version-history scrubbing
        // is done separately, as a full sweep over ALL groups below, because a group the user has
        // LEFT still carries them in its historical versions yet is not returned by
        // getWithFilter("users", id) (that index reflects only current membership).
        List<String> stakeholderIds = new ArrayList<>();
        for (Stakeholder s : stakeholderCrudService.getWithFilter("users", id)) {
            stakeholderService.removeMember(s.getId(), id);
            stakeholderService.removeAdmin(s.getId(), id);
            stakeholderIds.add(s.getId());
        }

        List<String> coordinatorIds = new ArrayList<>();
        for (Coordinator c : coordinatorCrudService.getWithFilter("users", id)) {
            coordinatorService.removeMember(c.getId(), id);
            coordinatorService.removeAdmin(c.getId(), id);
            coordinatorIds.add(c.getId());
        }

        List<String> administratorIds = new ArrayList<>();
        for (Administrator a : administratorCrudService.getWithFilter("users", id)) {
            administratorService.removeMember(a.getId(), id);
            administratorIds.add(a.getId());
        }

        // --- Scrub the user out of EVERY group's version history (current AND left groups) ---
        // The predicate removes the user from members/admins; it is a no-op (returns false,
        // nothing written) for groups the user never belonged to, so a blanket sweep is safe and
        // idempotent. Runs after the live removals above so it also catches the extra
        // "pre-removal" version snapshots those removals create.
        scrubAllOfType(stakeholderCrudService, (Stakeholder g) -> removeUserFromGroup(g, userId));
        scrubAllOfType(coordinatorCrudService, (Coordinator g) -> removeUserFromGroup(g, userId));
        scrubAllOfType(administratorCrudService, (Administrator g) -> removeUserFromGroup(g, userId));

        // Anonymize user identity from all survey answer history/metadata (current + versions).
        // A user can appear in an old version's history without appearing in the current payload
        // (e.g. later edited out by someone else), so every survey answer's versions are checked.
        int surveyAnswersAnonymized = scrubAllOfType(surveyAnswerCrudService,
                (SurveyAnswer a) -> scrubSurveyAnswerPii(a, userId));

        // News item authorship (metadata.createdBy/modifiedBy). Persisted through saveScrubbed
        // rather than update(): update() restores the stored metadata block and then re-stamps
        // modifiedBy from the security context, so routing the scrub through it would put the
        // purged address back and then replace it with the purging admin's.
        int newsItemsAnonymized = scrubAllOfType(newsItemService,
                (NewsItem n) -> scrubMetadata(n.getMetadata(), userId),
                newsItemService::saveScrubbed);

        // Survey definitions (Model) carry createdBy/modifiedBy as FLAT top-level fields rather than
        // inside a Metadata block, and Model is not Identifiable, so scrubAllOfType cannot take it.
        int surveyDefinitionsAnonymized = scrubAllUntyped(
                ModelService.MODEL_RESOURCE_TYPE_NAME,
                modelService.browse(sweepFilter()).getResults(),
                Model::getId,
                (Model m) -> scrubIdentityPair(m::getCreatedBy, m::setCreatedBy,
                        m::getModifiedBy, m::setModifiedBy, userId));

        // Documents. Metadata only: docInfo.authors.name/orcid and the harvested text/paragraphs are
        // bibliographic data about third parties in publicly harvested documents, not platform-user
        // identity, and are deliberately left alone.
        int documentsAnonymized = scrubAllUntyped(
                DOCUMENT_RESOURCE_TYPE,
                genericResourceService.<Document>getResults(sweepFilter(DOCUMENT_RESOURCE_TYPE)).getResults(),
                Document::getId,
                (Document d) -> scrubMetadata(d.getMetadata(), userId));

        // Anonymize comment authorship and @mentions in survey comments
        commentService.anonymizeUser(id, DELETED_USER_PLACEHOLDER);

        // Erase the user from the messaging service's threads. That service owns its own MongoDB,
        // so this HTTP call is the only path to the name and email it holds at rest. Failures are
        // deliberately not caught: aborting here leaves the User record intact for a re-run, which
        // beats deleting the account while its messaging PII survives. The operation is idempotent.
        int messagingThreadsAnonymized = messagingClient.anonymizeUser(id).block(MESSAGING_ERASURE_TIMEOUT);

        // Safety net: remove any remaining permissions
        permissionService.removeAll(id);

        // TODO: switch Spring Session to indexed mode (spring.session.redis.repository-type=indexed) so this user's Redis-backed HTTP sessions can be found by principal and deleted here.

        // Report what the purge touched, for audit/compliance purposes. Logged before the
        // final delete() so the report is captured even if that last step fails.
        // Deliberately omits the purged user's id/email from the log line — logging the
        // identifier being purged would itself retain the PII this method exists to remove.
        // The subject reference is a keyed HMAC, so it answers "was this person purged?" without
        // storing a readable address; it is pseudonymous, not anonymous.
        // TODO: write this to a durable erasure register (subject_ref, timestamp, requested_by, executed_by, scope, outcome) — archived logs are deleted after 180 days, but a complaint to a supervisory authority can arrive long after; pending the DPO's hash-vs-plaintext decision, which does not block the build since the column is a varchar either way.
        logger.info("Purge report: removed from stakeholder group(s) {}, coordinator group(s) {}, " +
                        "administrator group(s) {}; anonymized {} survey answer(s), {} news item(s), {} survey " +
                        "definition(s), {} document(s), {} messaging thread(s); anonymized comment authorship " +
                        "and @mentions; removed residual permissions. subject={}",
                stakeholderIds, coordinatorIds, administratorIds, surveyAnswersAnonymized, newsItemsAnonymized,
                surveyDefinitionsAnonymized, documentsAnonymized, messagingThreadsAnonymized, subjectRef);

        // Scrub the User resource's own version history before delete(id) below (which only
        // removes the current record; version rows persist and must be scrubbed in place).
        // Clears every identity field, nulls the whole profile block (future-proof: any new
        // profile field is dropped automatically), and clears forwardEmails. policiesAccepted is
        // kept as consent proof. Must run before delete(id): a deleted resource is no longer
        // fetchable via getResource.
        anonymizeVersions(this, id, (User u) -> {
            u.setSub(null);
            u.setEmail(null);
            u.setName(DELETED_FIELD_PLACEHOLDER);
            u.setSurname(DELETED_FIELD_PLACEHOLDER);
            u.setFullname(DELETED_USER_PLACEHOLDER);
            u.setProfile(null);
            if (u.getSettings() != null && u.getSettings().getNotificationPreferences() != null) {
                u.getSettings().getNotificationPreferences().setForwardEmails(null);
            }
            return true;
        });

        // Delete the user record
        delete(id);
    }

    /**
     * Rewrites every historical registry-core {@link Version} payload of a resource in place.
     * There is no API to delete a Version row (registry-core keeps version history append-only),
     * so erasure here means overwriting the PII inside the row, not removing the row itself —
     * the audit trail (row count, timestamps) stays intact.
     *
     * @param anonymizer mutates the deserialized version payload in place and returns whether
     *                   anything changed; only changed versions are written back.
     */
    private <T extends Identifiable> void anonymizeVersions(CrudService<T> crudService, String resourceId, Predicate<T> anonymizer) {
        anonymizeVersions(crudService.getResource(resourceId), anonymizer);
    }

    /**
     * As above, but taking the {@link Resource} directly. Split out because {@code Document} and
     * {@code Model} are not {@link Identifiable} and have no typed {@link CrudService} to obtain it
     * from — for those, resolve it the same two steps {@code AbstractCrudService#getResource} uses:
     * {@code resourceService.getResource(searchResource(<type>, id, true).getId())}.
     */
    private <T> void anonymizeVersions(Resource resource, Predicate<T> anonymizer) {
        List<Version> versions = resource.getVersions();
        if (versions == null) {
            return;
        }
        for (Version version : versions) {
            resource.setPayload(version.getPayload());
            resource.setResourceTypeName(version.getResourceTypeName());
            resource.setResourceType(version.getResourceType());
            T obj = (T) parserPool.deserialize(resource, getClassFromResourceType(version.getResourceTypeName()));
            if (anonymizer.test(obj)) {
                version.setPayload(parserPool.serialize(obj,
                        ParserService.ParserServiceTypes.fromString(version.getResourceType().getPayloadType())));
                versionService.updateVersion(version);
            }
        }
    }

    /**
     * Full-sweep erasure helper: iterates EVERY resource of the given type, scrubs the current
     * payload (persisting only when the predicate reports a change), and rewrites the resource's
     * entire version history. The predicate mutates its argument in place and returns whether
     * anything changed, so it is a safe no-op for resources the user never touched — making a
     * blanket sweep idempotent. Used for groups, survey answers and news items; resource types with
     * no typed {@link CrudService} go through {@link #scrubAllUntyped} instead.
     *
     * @return how many current payloads were actually modified.
     */
    private <T extends Identifiable<String>> int scrubAllOfType(CrudService<T> crudService, Predicate<T> scrub) {
        return scrubAllOfType(crudService, scrub, crudService::update);
    }

    /**
     * As above, but writing the scrubbed payload through {@code persist} instead of the service's
     * own {@code update}. News items need this: {@code NewsItemService#update} restores the stored
     * metadata block and then re-stamps {@code modifiedBy} from the security context, so routing a
     * scrub through it would put back the purged user's address and then overwrite it with the
     * identity of whoever is running the purge — trading one person's PII for another's.
     */
    private <T extends Identifiable<String>> int scrubAllOfType(CrudService<T> crudService, Predicate<T> scrub,
                                                                BiConsumer<String, T> persist) {
        int modified = 0;
        for (T resource : crudService.getAll(sweepFilter()).getResults()) {
            if (scrub.test(resource)) {
                persist.accept(resource.getId(), resource);
                modified++;
            }
            anonymizeVersions(crudService, resource.getId(), scrub);
        }
        return modified;
    }

    /**
     * A sweep filter sized to Elasticsearch's default max result window. 10000 is that default, not
     * an arbitrary cap; none of the swept resource types comes close to it.
     */
    private static FacetFilter sweepFilter() {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        return filter;
    }

    private static FacetFilter sweepFilter(String resourceType) {
        FacetFilter filter = sweepFilter();
        filter.setResourceType(resourceType);
        return filter;
    }

    /**
     * {@link #scrubAllOfType} for resource types with no typed {@link CrudService}: {@code Model} and
     * {@code Document} are not {@link Identifiable}, so they are enumerated and written through the
     * untyped {@link GenericResourceService} instead.
     *
     * <p>Writing directly rather than through the type's own service is deliberate in both cases.
     * {@code DefaultModelService#update} re-stamps {@code modificationDate}, mutates section
     * structure and runs validation that can throw on models dating from 2021–2022 — none of which
     * should happen for an administrative erasure — and it is the join point for the
     * {@code SurveyAspect} advice that can email every stakeholder on a deadline change or reopening.
     * {@code ResourcesService#update} likewise sets {@code curated = true} and re-stamps
     * {@code modifiedBy} from the security context.
     *
     * @return how many current payloads were modified.
     */
    private <T> int scrubAllUntyped(String resourceType, List<T> resources,
                                    java.util.function.Function<T, String> idOf, Predicate<T> scrub) {
        int modified = 0;
        for (T resource : resources) {
            String id = idOf.apply(resource);
            if (scrub.test(resource)) {
                updateUntyped(resourceType, id, resource);
                modified++;
            }
            anonymizeVersions(untypedResource(resourceType, id), scrub);
        }
        return modified;
    }

    /**
     * Resolves the {@link Resource} behind an untyped resource id, the same two steps
     * {@code AbstractCrudService#getResource} uses internally.
     */
    private Resource untypedResource(String resourceType, String id) {
        return resourceService.getResource(searchResource(resourceType, id, true).getId());
    }

    /**
     * {@code GenericResourceService#update} declares three checked reflection exceptions. They are
     * wrapped and rethrown rather than logged and swallowed: {@code purge()} is documented as safe to
     * re-run after a partial failure, so aborting loudly is both safe and correct — an erasure that
     * only half-succeeded must not report as a clean run.
     */
    private <T> void updateUntyped(String resourceType, String id, T resource) {
        try {
            genericResourceService.update(resourceType, id, resource);
        } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException e) {
            throw new ServiceException(
                    String.format("Failed to scrub %s '%s' during purge.", resourceType, id), e);
        }
    }

    /**
     * Rewrites a comma-delimited list of user ids, replacing every occurrence of {@code userId} with
     * {@link #DELETED_USER_PLACEHOLDER}.
     *
     * <p>Needed because {@code SurveyAnswerRevisionsAggregation#updateHistory} writes
     * {@code metadata.modifiedBy} as a comma-joined list of everyone who edited during a session, so
     * a whole-string {@code equals} never matches it and the address survives the purge.
     *
     * <p>The placeholder is de-duplicated globally rather than only where it repeats consecutively:
     * purging two people at different times would otherwise leave two identical markers. Nothing is
     * lost by collapsing them — the per-editor rows in {@code history.entries[].editors[]} are
     * scrubbed individually and still record how many distinct people edited. Real ids are left
     * exactly as they are, repeats included.
     *
     * @return the rewritten value, or {@code null} when nothing matched — so a re-run writes nothing
     *         and the purge stays idempotent.
     */
    private static String scrubDelimited(String value, String userId) {
        if (value == null) {
            return null;
        }
        boolean matched = false;
        List<String> tokens = new ArrayList<>();
        // Tolerant of a spaced separator too, in case the writer ever emits ", " instead of ",".
        for (String token : value.split("\\s*,\\s*")) {
            if (token.isEmpty()) {
                continue;
            }
            if (userId.equals(UserIds.normalize(token))) {
                matched = true;
                token = DELETED_USER_PLACEHOLDER;
            }
            if (DELETED_USER_PLACEHOLDER.equals(token) && tokens.contains(DELETED_USER_PLACEHOLDER)) {
                continue;
            }
            tokens.add(token);
        }
        return matched ? String.join(",", tokens) : null;
    }

    /**
     * Scrubs a createdBy/modifiedBy pair reached through accessors rather than a {@link Metadata}
     * block — {@code Model} carries them as flat top-level fields. One predicate, two shapes.
     *
     * @return whether anything changed.
     */
    private static boolean scrubIdentityPair(Supplier<String> getCreatedBy, Consumer<String> setCreatedBy,
                                             Supplier<String> getModifiedBy, Consumer<String> setModifiedBy,
                                             String userId) {
        boolean modified = false;
        String createdBy = scrubDelimited(getCreatedBy.get(), userId);
        if (createdBy != null) {
            setCreatedBy.accept(createdBy);
            modified = true;
        }
        String modifiedBy = scrubDelimited(getModifiedBy.get(), userId);
        if (modifiedBy != null) {
            setModifiedBy.accept(modifiedBy);
            modified = true;
        }
        return modified;
    }

    /**
     * {@link #scrubIdentityPair} for the common case of a {@link Metadata} block. Null-safe.
     */
    private static boolean scrubMetadata(Metadata metadata, String userId) {
        return metadata != null && scrubIdentityPair(metadata::getCreatedBy, metadata::setCreatedBy,
                metadata::getModifiedBy, metadata::setModifiedBy, userId);
    }

    /**
     * Removes {@code userId} from a group's members and admins sets. Null-safe; returns whether
     * anything changed, so it doubles as the scrub predicate for group version history.
     */
    private static boolean removeUserFromGroup(UserGroup group, String userId) {
        boolean removed = group.getMembers() != null && group.getMembers().remove(userId);
        if (group.getAdmins() != null && group.getAdmins().remove(userId)) {
            removed = true;
        }
        return removed;
    }

    /**
     * Scrubs {@code id} out of a SurveyAnswer's editor/creator/modifier fields, including the
     * deprecated {@link HistoryEntry#getUserId()} field (pre-dates the {@code editors} list;
     * older registry-core version snapshots may still carry the raw id there instead).
     *
     * @return whether anything was changed.
     */
    private boolean scrubSurveyAnswerPii(SurveyAnswer answer, String id) {
        boolean modified = false;
        if (answer.getHistory() != null && answer.getHistory().getEntries() != null) {
            for (HistoryEntry entry : answer.getHistory().getEntries()) {
                if (id.equals(entry.getUserId())) {
                    entry.setUserId(DELETED_USER_PLACEHOLDER);
                    modified = true;
                }
                if (entry.getEditors() != null) {
                    for (Editor editor : entry.getEditors()) {
                        if (id.equals(editor.getUser())) {
                            editor.setUser(DELETED_USER_PLACEHOLDER);
                            modified = true;
                        }
                    }
                }
            }
        }
        // Both go through scrubDelimited rather than an exact match: modifiedBy is written as a
        // comma-joined list of a session's editors, so a whole-string equals never fires against it.
        // createdBy is single-valued today, but sharing the code path costs nothing and survives
        // anyone later reusing the aggregation logic for creation.
        if (scrubMetadata(answer.getMetadata(), id)) {
            modified = true;
        }
        return modified;
    }

    private UserInfo createUserInfo(User user) {
        UserInfo info = new UserInfo();
        info.setUser(user);
        info.setAdmin(applicationProperties.getAdmins().contains(user.getEmail()));
        info.setStakeholders(new HashSet<>());
        info.setCoordinators(new HashSet<>());
        info.setAdministrators(new HashSet<>());

        info.getStakeholders().addAll(stakeholderCrudService.getWithFilter("users", user.getId()));
        info.getCoordinators().addAll(coordinatorCrudService.getWithFilter("users", user.getId()));
        info.getAdministrators().addAll(administratorCrudService.getWithFilter("users", user.getId()));
        return info;
    }
}
