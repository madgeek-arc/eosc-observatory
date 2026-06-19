package eu.openaire.observatory.domain;

import eu.openaire.observatory.service.Identifiable;

import java.util.List;

public class StakeholderIndicatorsOverride implements Identifiable<String> {

    private String id;
    private String stakeholderId;
    private List<String> addedIndicatorIds;
    private List<String> removedIndicatorIds;

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

    public List<String> getAddedIndicatorIds() {
        return addedIndicatorIds;
    }

    public void setAddedIndicatorIds(List<String> addedIndicatorIds) {
        this.addedIndicatorIds = addedIndicatorIds;
    }

    public List<String> getRemovedIndicatorIds() {
        return removedIndicatorIds;
    }

    public void setRemovedIndicatorIds(List<String> removedIndicatorIds) {
        this.removedIndicatorIds = removedIndicatorIds;
    }
}
