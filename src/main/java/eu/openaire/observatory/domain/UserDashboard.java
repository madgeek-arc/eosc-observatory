package eu.openaire.observatory.domain;

import eu.openaire.observatory.service.Identifiable;

import java.util.List;

public class UserDashboard implements Identifiable<String> {

    private String id; // owner's normalized email; one dashboard per user
    private List<DashboardItem> items;
    private Metadata metadata;

    public UserDashboard() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public List<DashboardItem> getItems() {
        return items;
    }

    public void setItems(List<DashboardItem> items) {
        this.items = items;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
}
