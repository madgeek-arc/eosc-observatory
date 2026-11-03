package eu.openaire.observatory.widget;

/**
 * One toggleable card on a stakeholder-facing page (a chart, a number/percentage figure,
 * or a plain informational card), with its section grouping and visibility.
 */
public class Widget {

    private String id;
    private String label;
    private boolean visible;
    private String format;
    private String group;

    public Widget() {
    }

    public Widget(Widget other) {
        this.id = other.id;
        this.label = other.label;
        this.visible = other.visible;
        this.format = other.format;
        this.group = other.group;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
