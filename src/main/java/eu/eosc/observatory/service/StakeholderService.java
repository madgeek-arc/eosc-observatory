package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.dto.StakeholderMembers;

import java.util.Set;

public interface StakeholderService extends CrudItemService<Stakeholder> {

    StakeholderMembers getMembers(String id);

    Stakeholder updateStakeholderAndUserPermissions(String stakeholderId, Stakeholder stakeholder);

    Stakeholder updateContributors(String stakeholderId, Set<String> emails);

    StakeholderMembers addContributor(String stakeholderId, String email);

    StakeholderMembers removeContributor(String stakeholderId, String email);

    Stakeholder updateManagers(String stakeholderId, Set<String> emails);

    StakeholderMembers addManager(String stakeholderId, String email);

    StakeholderMembers removeManager(String stakeholderId, String email);
}
