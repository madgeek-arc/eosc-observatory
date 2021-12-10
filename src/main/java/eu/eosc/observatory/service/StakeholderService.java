package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.dto.StakeholderMembers;

import java.util.List;

public interface StakeholderService extends CrudItemService<Stakeholder> {

    StakeholderMembers getMembers(String id);

    Stakeholder updateContributors(String stakeholderId, List<String> emails);
    Stakeholder addContributor(String stakeholderId, String email);
    Stakeholder removeContributor(String stakeholderId, String email);

    Stakeholder updateManagers(String stakeholderId, List<String> emails);
    Stakeholder addManager(String stakeholderId, String email);
    Stakeholder removeManager(String stakeholderId, String email);
}
