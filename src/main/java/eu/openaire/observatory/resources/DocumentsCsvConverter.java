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
package eu.openaire.observatory.resources;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.openaire.observatory.resources.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentsCsvConverter {

    private static final Logger logger = LoggerFactory.getLogger(DocumentsCsvConverter.class);

    private final ObjectMapper mapper;

    public DocumentsCsvConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String toTsv(List<Document> documents) {
        List<JsonNode> docs = documents
                .stream()
                .peek(i -> { // append id/url fields to tsv
                    ObjectNode reorder = mapper.createObjectNode();
                    reorder.put("id", i.getId());
                    reorder.put("url", i.getUrl());
                    if (!i.getDocInfo().isNull()) { // skip if NullNode
                        reorder.setAll((ObjectNode) i.getDocInfo());
                    }
                    i.setDocInfo(reorder);
                })
                .map(Document::getDocInfo)
                .toList();
        ArrayNode array = mapper.createArrayNode();
        array.addAll(docs);

        String data;
        try {
            data = JsonNodeToCsv.toTsv(array);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            data = "";
        }
        return data;
    }
}
