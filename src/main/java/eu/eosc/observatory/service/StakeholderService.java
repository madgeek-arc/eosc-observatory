package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.dto.StakeholderMembers;

import java.util.List;

public interface StakeholderService extends CrudItemService<Stakeholder> {

    StakeholderMembers getMembers(String id);

    Stakeholder updateContributors(String stakeholderId, List<String> emails);

    Stakeholder updateManagers(String stakeholderId, List<String> emails);
}
