package eu.eosc.observatory.mappers;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.dto.StakeholderDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StakeholderMapper {

    Stakeholder toStakeholder(StakeholderDTO source);

    StakeholderDTO toDTO(Stakeholder source);
}
