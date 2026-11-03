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

import java.util.Map;

/**
 * Source of the values of external questions (Eurostat, OpenAIRE, COARA, ...).
 */
public interface ExternalValueProvider {

    /**
     * Returns the values of an external question for a data year.
     *
     * @param fieldName the name of the question's field in the survey model, e.g. "Question57-0"
     * @param year      the data year, e.g. "2024"
     * @return the values keyed by the two-letter country code, empty when there are none
     */
    Map<String, String> getValues(String fieldName, String year);
}
