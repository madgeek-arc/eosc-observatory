package eu.openaire.observatory.widget;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.openaire.observatory.service.Identifiable;

import java.util.List;

/** A stakeholder's overrides of one named catalog's default widget visibility. */
public class DashboardOverrides implements Identifiable<String> {

    private String id;
    private String code;
    private String stakeholderId;
    private List<Widget> widgets;

    public DashboardOverrides() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStakeholderId() {
        return stakeholderId;
    }

    public void setStakeholderId(String stakeholderId) {
        this.stakeholderId = stakeholderId;
    }

    @JsonProperty("indicators")
    public List<Widget> getWidgets() {
        return widgets;
    }

    @JsonProperty("indicators")
    public void setWidgets(List<Widget> widgets) {
        this.widgets = widgets;
    }
}
