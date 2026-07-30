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
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@Service
public class UserServiceImpl extends AbstractCrudService<User> implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    public static final String DELETED_USER_PLACEHOLDER = "[Deleted User]";
    // For decomposed name fields (User#name/#surname) rather than a whole identity string:
    // some call sites (e.g. UserDTO, StakeholderServiceImpl#getManagers) filter on non-null
    // name/surname or concatenate them, so both must stay non-null and non-duplicated.
    public static final String DELETED_FIELD_PLACEHOLDER = "[Deleted]";

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
     * Not wrapped in a single transaction: the steps span both the registry's
     * Postgres+Elasticsearch-backed group/permission storage and this service's own
     * JPA-backed comment/user storage, so no single transaction manager could cover all of it.
     * Instead, every step here is idempotent (group/permission removal and the placeholder
     * rewrites are no-ops when reapplied), so on partial failure it is safe to simply call
     * purge() again — it will pick up wherever it left off. The one exception is the final
     * delete(id): if a prior run already completed, retrying throws ResourceNotFoundException,
     * which is the expected/idiomatic response for deleting an already-deleted resource.
     */
    @Override
    public void purge(String id) throws ResourceNotFoundException {
        // Normalize to lowercase up front: emails/ids are stored lowercase everywhere
        // (see User#setEmail, User#getId), but this id comes from a path variable and
        // isn't guaranteed to match that casing. Every comparison/query below is exact-case.
        id = id.toLowerCase();
        // Captured by the anonymizeVersions(...) lambdas below, which need an effectively-final reference.
        final String userId = id;

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

        // NOTE (follow-up, not covered here): user identity in "news_item" and "document" registry
        // resources (metadata.createdBy/modifiedBy). NewsItemService#update restores the existing
        // metadata and re-stamps modifiedBy from the security context, so the generic version-scrub
        // can't clean the *current* news_item record; Document is not Identifiable and has no typed
        // CrudService (it is managed via GenericResourceService). Both need a dedicated
        // metadata-aware scrub, tracked alongside the external messaging-service erasure.

        // Anonymize comment authorship and @mentions in survey comments
        commentService.anonymizeUser(id, DELETED_USER_PLACEHOLDER);

        // Safety net: remove any remaining permissions
        permissionService.removeAll(id);

        // TODO: switch Spring Session to indexed mode (spring.session.redis.repository-type=indexed) so this user's Redis-backed HTTP sessions can be found by principal and deleted here.

        // Report what the purge touched, for audit/compliance purposes. Logged before the
        // final delete() so the report is captured even if that last step fails.
        // Deliberately omits the purged user's id/email from the log line — logging the
        // identifier being purged would itself retain the PII this method exists to remove.
        logger.info("Purge report: removed from stakeholder group(s) {}, coordinator group(s) {}, " +
                        "administrator group(s) {}; anonymized {} survey answer(s); anonymized comment authorship " +
                        "and @mentions; removed residual permissions.",
                stakeholderIds, coordinatorIds, administratorIds, surveyAnswersAnonymized);

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
        Resource resource = crudService.getResource(resourceId);
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
     * blanket sweep idempotent. Reused for groups, survey answers and news items.
     *
     * @return how many current payloads were actually modified.
     */
    private <T extends Identifiable<String>> int scrubAllOfType(CrudService<T> crudService, Predicate<T> scrub) {
        FacetFilter filter = new FacetFilter();
        // 10000 is Elasticsearch's default max result window, not an arbitrary cap; these resource
        // types (groups / survey answers / news items) never approach it.
        filter.setQuantity(10000);
        int modified = 0;
        for (T resource : crudService.getAll(filter).getResults()) {
            if (scrub.test(resource)) {
                crudService.update(resource.getId(), resource);
                modified++;
            }
            anonymizeVersions(crudService, resource.getId(), scrub);
        }
        return modified;
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
        if (answer.getMetadata() != null) {
            if (id.equals(answer.getMetadata().getCreatedBy())) {
                answer.getMetadata().setCreatedBy(DELETED_USER_PLACEHOLDER);
                modified = true;
            }
            if (id.equals(answer.getMetadata().getModifiedBy())) {
                answer.getMetadata().setModifiedBy(DELETED_USER_PLACEHOLDER);
                modified = true;
            }
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
