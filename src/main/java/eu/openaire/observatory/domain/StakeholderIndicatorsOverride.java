package eu.openaire.observatory.domain;

import eu.openaire.observatory.service.Identifiable;

import java.util.List;

public class StakeholderIndicatorsOverride implements Identifiable<String> {

    private String id;
    private String stakeholderId;
    private List<Indicator> indicators;

    public StakeholderIndicatorsOverride() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getStakeholderId() {
        return stakeholderId;
    }

    public void setStakeholderId(String stakeholderId) {
        this.stakeholderId = stakeholderId;
    }

    public List<Indicator> getIndicators() {
        return indicators;
    }

    public void setIndicators(List<Indicator> indicators) {
        this.indicators = indicators;
    }
}
