package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.dto.StakeholderMembers;

import java.util.List;

public interface StakeholderService extends CrudItemService<Stakeholder> {

    StakeholderMembers getMembers(String id);

    Stakeholder updateContributors(String stakeholderId, List<String> emails);
    StakeholderMembers addContributor(String stakeholderId, String email);
    StakeholderMembers removeContributor(String stakeholderId, String email);

    Stakeholder updateManagers(String stakeholderId, List<String> emails);
    StakeholderMembers addManager(String stakeholderId, String email);
    StakeholderMembers removeManager(String stakeholderId, String email);
}
