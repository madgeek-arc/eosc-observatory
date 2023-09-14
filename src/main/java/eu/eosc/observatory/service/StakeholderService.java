package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;

import java.util.Set;

public interface StakeholderService extends CrudService<Stakeholder>, UserGroupService {

    Stakeholder updateStakeholderAndUserPermissions(String stakeholderId, Stakeholder stakeholder);

    Stakeholder updateContributors(String stakeholderId, Set<String> emails);

    Stakeholder updateManagers(String stakeholderId, Set<String> emails);

}
