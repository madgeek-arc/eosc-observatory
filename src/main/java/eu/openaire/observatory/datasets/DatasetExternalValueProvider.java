/*
 * Copyright 2021-2026 OpenAIRE AMKE
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

package eu.openaire.observatory.datasets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads the values of external questions from the external datasets ({@link DatasetEntry}),
 * which are uploaded by hand through {@code POST /datasets}.
 */
@Service
public class DatasetExternalValueProvider implements ExternalValueProvider {

    private static final Logger logger = LoggerFactory.getLogger(DatasetExternalValueProvider.class);

    /*
     * Which dataset fills each external question: the key is the name of the question's field in the
     * survey model, the value is the name and authority of its dataset entries (as written by CustomIngestService).
     * External questions missing from here have no dataset, so their values stay empty.
     */
    private static final Map<String, Dataset> DATASETS_BY_FIELD = Map.of(
            "Question1-0", new Dataset("researchers", "EUROSTAT"),
            "Question57-0", new Dataset("publications", "OpenAIRE"),
            "Question69-0", new Dataset("datasets", "OpenAIRE"),
            "Question73-0", new Dataset("software", "OpenAIRE"),
            "Question77-0", new Dataset("services", "EOSC"),
            "Question97-0", new Dataset("signatories", "COARA")
    );

    private final DatasetService datasetService;

    public DatasetExternalValueProvider(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @Override
    public Map<String, String> getValues(String fieldName, String year) {
        Dataset dataset = DATASETS_BY_FIELD.get(fieldName);
        if (dataset == null) {
            return Map.of();
        }
        List<DatasetEntry> entries = datasetService.getAll(new DatasetEntry(year, null, dataset.name(), dataset.authority(), null, null));

        // the values are kept as they are, "-" included, since that is how the source marks a missing value
        Map<String, String> values = new HashMap<>();
        for (DatasetEntry entry : entries) {
            if (entry.getCountry() == null) {
                continue;
            }
            String country = entry.getCountry().toUpperCase(Locale.ROOT);
            if (values.putIfAbsent(country, entry.getValue()) != null) {
                logger.warn("More than one '{}' ({}) entry for country '{}' in year {}, keeping the first one.",
                        dataset.name(), dataset.authority(), country, year);
            }
        }
        return values;
    }

    private record Dataset(String name, String authority) {
    }
}
