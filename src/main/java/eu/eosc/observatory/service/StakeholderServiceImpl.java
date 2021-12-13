package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.StakeholderMembers;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StakeholderServiceImpl extends AbstractCrudItemService<Stakeholder> implements StakeholderService {

    private static final Logger logger = LogManager.getLogger(StakeholderServiceImpl.class);

    private static final String RESOURCE_TYPE = "stakeholder";

    private final CrudItemService<User> userService;

    @Autowired
    public StakeholderServiceImpl(ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  ParserService parserService,
                                  CrudItemService<User> userService) {
        super(resourceTypeService, resourceService, searchService, parserService);
        this.userService = userService;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String createId(Stakeholder stakeholder) {
        String idSuffix;
        if (Stakeholder.StakeholderType.fromString(stakeholder.getType()) == Stakeholder.StakeholderType.COUNTRY) {
            idSuffix = stakeholder.getCountry();
        } else {
            idSuffix = stakeholder.getAssociationMember();
        }
        return String.format("sh-%s-%s", stakeholder.getType(), idSuffix);
    }

    @Override
    public StakeholderMembers getMembers(String id) {
        Stakeholder stakeholder = this.get(id);
        return getMembers(stakeholder);
    }

    private StakeholderMembers getMembers(Stakeholder stakeholder) {
        List<User> managers = new ArrayList<>();
        List<User> contributors = new ArrayList<>();
        if (stakeholder.getManagers() != null) {
            managers = stakeholder.getManagers()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::getUser)
                    .collect(Collectors.toList());
        }
        if (stakeholder.getContributors() != null) {
            contributors = stakeholder.getContributors()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::getUser)
                    .collect(Collectors.toList());
        }

        return new StakeholderMembers(contributors, managers);
    }

    @Override
    public Stakeholder updateContributors(String stakeholderId, List<String> emails) {
        Stakeholder stakeholder = get(stakeholderId);
        stakeholder.setContributors(emails);
        return update(stakeholderId, stakeholder);
    }

    @Override
    public StakeholderMembers addContributor(String stakeholderId, String email) {
        Stakeholder stakeholder = get(stakeholderId);
        if (stakeholder.getContributors() == null) {
            stakeholder.setContributors(new ArrayList<>());
        }
        stakeholder.getContributors().add(email);
        stakeholder = update(stakeholderId, stakeholder);
        return getMembers(stakeholder);
    }

    @Override
    public StakeholderMembers removeContributor(String stakeholderId, String email) {
        Stakeholder stakeholder = get(stakeholderId);
        stakeholder.getContributors().remove(email);
        stakeholder = update(stakeholderId, stakeholder);
        return getMembers(stakeholder);
    }

    @Override
    public Stakeholder updateManagers(String stakeholderId, List<String> emails) {
        Stakeholder stakeholder = get(stakeholderId);
        stakeholder.setManagers(emails);
        return update(stakeholderId, stakeholder);
    }

    @Override
    public StakeholderMembers addManager(String stakeholderId, String email) {
        Stakeholder stakeholder = get(stakeholderId);
        if (stakeholder.getManagers() == null) {
            stakeholder.setManagers(new ArrayList<>());
        }
        stakeholder.getManagers().add(email);
        stakeholder = update(stakeholderId, stakeholder);
        return getMembers(stakeholder);
    }

    @Override
    public StakeholderMembers removeManager(String stakeholderId, String email) {
        Stakeholder stakeholder = get(stakeholderId);
        stakeholder.getManagers().remove(email);
        stakeholder = update(stakeholderId, stakeholder);
        return getMembers(stakeholder);
    }

    private User getUser(String email) {
        User user = null;
        try {
            user = userService.get(email);
        } catch (ResourceNotFoundException e) {
            logger.debug("User not found in DB");
            user = new User();
            user.setEmail(email);
        }
        return user;
    }
}
