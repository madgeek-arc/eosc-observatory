package eu.openaire.observatory.domain;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.service.Identifiable;

import java.util.List;

public class UserDashboard implements Identifiable<String> {

    private String id; // owner's normalized email
    private List<DashboardItem> items;
    private List<String> countries; // nullable/empty = no country filter, applies to every item
    private Integer yearFrom; // required only if at least one item's indicator supports a time series
    private Integer yearTo; // required whenever items is non-empty
    private List<AggregationType> seriesAggregations; // nullable/empty = none requested, applies to every item that allows each value
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

    public List<String> getCountries() {
        return countries;
    }

    public void setCountries(List<String> countries) {
        this.countries = countries;
    }

    public Integer getYearFrom() {
        return yearFrom;
    }

    public void setYearFrom(Integer yearFrom) {
        this.yearFrom = yearFrom;
    }

    public Integer getYearTo() {
        return yearTo;
    }

    public void setYearTo(Integer yearTo) {
        this.yearTo = yearTo;
    }

    public List<AggregationType> getSeriesAggregations() {
        return seriesAggregations;
    }

    public void setSeriesAggregations(List<AggregationType> seriesAggregations) {
        this.seriesAggregations = seriesAggregations;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
}
