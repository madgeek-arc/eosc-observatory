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

import java.util.Map;

/**
 * Placeholder until the source of the external values is decided: it returns no values,
 * so the external questions export has empty value cells.
 * Replace it with a provider that reads the real source.
 */
@Service
public class NoExternalValueProvider implements ExternalValueProvider {

    private static final Logger logger = LoggerFactory.getLogger(NoExternalValueProvider.class);

    @Override
    public Map<String, String> getValues(String fieldName, String year) {
        logger.debug("No source of external values yet, '{}' for year {} stays empty.", fieldName, year);
        return Map.of();
    }
}
