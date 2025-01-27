/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
