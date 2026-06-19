package eu.openaire.observatory.domain;

import eu.openaire.observatory.service.Identifiable;

import java.util.List;

public class DefaultIndicators implements Identifiable<String> {

    private String id;
    private String type;
    private List<String> indicatorIds;

    public DefaultIndicators() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getIndicatorIds() {
        return indicatorIds;
    }

    public void setIndicatorIds(List<String> indicatorIds) {
        this.indicatorIds = indicatorIds;
    }
}
