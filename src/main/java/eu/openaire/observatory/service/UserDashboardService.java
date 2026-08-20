package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.DashboardItem;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.domain.UserDashboard;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.utils.UserIds;
import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One dashboard per user, keyed by the owner's normalized email. Private to its owner. */
@Service
public class UserDashboardService extends AbstractCrudService<UserDashboard> {

    private static final String RESOURCE_TYPE = "user_dashboard";

    private final IndicatorQueryValidator indicatorQueryValidator;
    private final ActorContext actorContext;

    public UserDashboardService(IndicatorQueryValidator indicatorQueryValidator,
                                ActorContext actorContext,
                                ResourceTypeService resourceTypeService,
                                ResourceService resourceService,
                                SearchService searchService,
                                VersionService versionService,
                                ParserService parserService,
                                ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.indicatorQueryValidator = indicatorQueryValidator;
        this.actorContext = actorContext;
    }

    @Override
    public String createId(UserDashboard resource) {
        return resource.getId();
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    /** Never persists on read — a user with no saved dashboard yet just sees an empty one. */
    public UserDashboard getOrCreateForCurrentUser(Authentication authentication) {
        return getOrCreate(User.getId(authentication));
    }

    /** Admin/support variant of {@link #getOrCreateForCurrentUser}: reads any user's dashboard by id. */
    public UserDashboard getOrCreate(String ownerId) {
        String id = UserIds.normalize(ownerId);
        try {
            return get(id);
        } catch (ResourceNotFoundException e) {
            UserDashboard dashboard = new UserDashboard();
            dashboard.setId(id);
            dashboard.setItems(List.of());
            return dashboard;
        }
    }

    /** Replaces the current user's full item list in one call. */
    public UserDashboard save(Authentication authentication, List<DashboardItem> items) {
        return save(User.getId(authentication), authentication, items);
    }

    /**
     * Admin/support variant of {@link #save(Authentication, List)}: replaces {@code ownerId}'s
     * dashboard on their behalf. {@code actor} (who may differ from {@code ownerId}) is recorded
     * in the resource's audit metadata as the one who actually performed the write.
     */
    public UserDashboard save(String ownerId, Authentication actor, List<DashboardItem> items) {
        String id = UserIds.normalize(ownerId);
        List<DashboardItem> normalized = normalizeAndValidate(items);

        UserDashboard dashboard = new UserDashboard();
        dashboard.setId(id);
        dashboard.setItems(normalized);

        try {
            UserDashboard existing = get(id);
            Metadata metadata = existing.getMetadata();
            metadata.setModifiedBy(User.getId(actor));
            metadata.setModificationDate(new Date());
            dashboard.setMetadata(metadata);
            return update(id, dashboard);
        } catch (ResourceNotFoundException e) {
            dashboard.setMetadata(new Metadata(actor));
            return add(dashboard);
        }
    }

    private List<DashboardItem> normalizeAndValidate(List<DashboardItem> items) {
        if (items == null) {
            return List.of();
        }

        List<DashboardItem> normalized = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (DashboardItem item : items) {
            if (item.getChartType() == null) {
                throw new ValidationException("Dashboard item chartType must not be null");
            }
            if (item.getQuery() == null) {
                throw new ValidationException("Dashboard item query must not be null");
            }
            indicatorQueryValidator.compile(item.getQuery(), actorContext);

            String itemId = (item.getId() == null || item.getId().isBlank())
                    ? UUID.randomUUID().toString()
                    : item.getId();
            if (!seenIds.add(itemId)) {
                throw new ValidationException("Duplicate dashboard item id: " + itemId);
            }
            item.setId(itemId);
            normalized.add(item);
        }
        return normalized;
    }
}
