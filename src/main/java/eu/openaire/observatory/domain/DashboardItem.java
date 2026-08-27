package eu.openaire.observatory.domain;

/** A reference to one {@code Indicator} on a user's dashboard. */
public class DashboardItem {

    private String id;
    private String indicatorId;

    public DashboardItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIndicatorId() {
        return indicatorId;
    }

    public void setIndicatorId(String indicatorId) {
        this.indicatorId = indicatorId;
    }
}
