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

package eu.openaire.observatory.resources.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentTemplateLoader {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTemplateLoader.class);

    private JsonNode template;
    private final ObjectMapper mapper;

    public DocumentTemplateLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode load() {
        if (template == null) {
            try (InputStream is = DocumentTemplateLoader.class.getClassLoader().getResourceAsStream("template.json")) {

                if (is != null) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    logger.debug("JSON Template:\n{}", content);
                    this.template = mapper.readTree(content);
                } else {
                    logger.error("Could not read template.json");
                    throw new IOException("Could not read template.json");
                }
            } catch (IOException e) {
                return null;
            }
        }
        return template;
    }
}