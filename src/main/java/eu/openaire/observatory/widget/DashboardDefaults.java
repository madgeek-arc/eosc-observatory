package eu.openaire.observatory.widget;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.openaire.observatory.service.Identifiable;

import java.util.List;

/** The default set of widgets and their visibility for a stakeholder type, within one named catalog. */
public class DashboardDefaults implements Identifiable<String> {

    private String id;
    private String code;
    private String name;
    private String type;
    private List<Widget> widgets;

    public DashboardDefaults() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
