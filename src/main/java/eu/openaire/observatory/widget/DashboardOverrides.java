package eu.openaire.observatory.widget;

import eu.openaire.observatory.service.Identifiable;

import java.util.List;

/** A stakeholder's overrides of one named catalog's default widget visibility. */
public class DashboardOverrides implements Identifiable<String> {

    private String id;
    private DashboardCode code;
    private String type;
    private String groupId;
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

    public DashboardCode getCode() {
        return code;
    }

    public void setCode(DashboardCode code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<Widget> widgets) {
        this.widgets = widgets;
    }
}
