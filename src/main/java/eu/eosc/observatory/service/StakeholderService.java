package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.GroupMembers;

import java.util.Set;

public interface StakeholderService extends CrudService<Stakeholder>, UserGroupService {

    GroupMembers getAllMembers(String id);

    Stakeholder updateStakeholderAndUserPermissions(String stakeholderId, Stakeholder stakeholder);

    Stakeholder updateContributors(String stakeholderId, Set<String> emails);

    GroupMembers addContributor(String stakeholderId, String email);

    GroupMembers removeContributor(String stakeholderId, String email);

    Stakeholder updateManagers(String stakeholderId, Set<String> emails);

    GroupMembers addManager(String stakeholderId, String email);

    GroupMembers removeManager(String stakeholderId, String email);
}
