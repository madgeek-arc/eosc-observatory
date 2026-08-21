package eu.openaire.observatory.domain;

import eu.openaire.observatory.indicator.model.ChartType;
import eu.openaire.observatory.indicator.query.IndicatorQuery;

public class DashboardItem {

    private String id;
    private String title;
    private ChartType chartType;
    private IndicatorQuery query;

    public DashboardItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ChartType getChartType() {
        return chartType;
    }

    public void setChartType(ChartType chartType) {
        this.chartType = chartType;
    }

    public IndicatorQuery getQuery() {
        return query;
    }

    public void setQuery(IndicatorQuery query) {
        this.query = query;
    }
}
