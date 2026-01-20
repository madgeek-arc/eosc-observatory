/*
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
package eu.openaire.observatory.resources.analyzer;

import eu.openaire.observatory.resources.analyzer.model.SurveyAnswerReference;
import eu.openaire.observatory.resources.analyzer.model.UrlReferences;
import eu.openaire.observatory.domain.SurveyAnswer;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SurveyAnswerUrlExtractor implements UrlExtractor<SurveyAnswer> {

    @Override
    public List<UrlReferences> extract(SurveyAnswer answer) {
        Map<String, List<String>> urls = extractUrls(null, answer.getAnswer(), "");
        List<UrlReferences> documentUrls = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : urls.entrySet()) {
            SurveyAnswerReference surveyAnswerReference = new SurveyAnswerReference(answer.getId(), entry.getValue());
            UrlReferences urlReferences = new UrlReferences(entry.getKey(), new LinkedHashSet<>());
            urlReferences.getReferences().add(surveyAnswerReference);
            documentUrls.add(urlReferences);
        }
        return documentUrls;
    }

    private Map<String, List<String>> extractUrls(Map<String, List<String>> urls, Object json, String key) {
        if (urls == null) {
            urls = new HashMap<>();
        }
        if (json instanceof String value) {
            if (value.matches("https?://[\\w\\-\\.\\:/%\\?&=]+")) {
                urls.putIfAbsent(value, new ArrayList<>());
                urls.get(value).add(key);
            }
        } else if (json instanceof List list) {
            for (Object entry : list) {
                extractUrls(urls, entry, key);
            }
        } else if (json instanceof Map) {
            for (Map.Entry<String, ?> entry : ((Map<String, ?>) json).entrySet()) {
                extractUrls(urls, entry, entry.getKey());
            }
        } else if (json instanceof Map.Entry<?,?> entry) {
            extractUrls(urls, entry.getValue(), (String) entry.getKey());
        }
        return urls;
    }
}
