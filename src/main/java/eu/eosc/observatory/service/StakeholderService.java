package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.dto.StakeholderMembers;

public interface StakeholderService extends CrudItemService<Stakeholder> {

    StakeholderMembers getMembers(String id);
}
