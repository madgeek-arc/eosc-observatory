package eu.eosc.observatory.statistics;

public class QueryRequest {

    private Series[] series;
    private boolean verbose;

    public Series[] getSeries() {
        return series;
    }

    public QueryRequest setSeries(Series[] series) {
        this.series = series;
        return this;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public QueryRequest setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public static class Series {
        Query query;

        public Query getQuery() {
            return query;
        }

        public Series setQuery(Query query) {
            this.query = query;
            return this;
        }
    }

    public static class Query {
        String name;
        String profile;

        public String getName() {
            return name;
        }

        public Query setName(String name) {
            this.name = name;
            return this;
        }

        public String getProfile() {
            return profile;
        }

        public Query setProfile(String profile) {
            this.profile = profile;
            return this;
        }
    }
}
