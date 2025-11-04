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
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.StreamSupport;

public class JsonNodeToCsv {

    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonNodeToCsv() {
    }

    public static String toCsv(JsonNode root) throws IOException {
        return toDelimited(root, ',');
    }

    public static String toTsv(JsonNode root) throws IOException {
        return toDelimited(root, '\t');
    }

    public static String toDelimited(JsonNode root, char delimiter) throws IOException {
        return toDelimited(root, delimiter, ';');
    }

    public static String toDelimited(JsonNode root, char delimiter, char arrayDelimiter) throws IOException {
        // Normalize input into a list of rows
        List<ObjectNode> rows = normalize(root);

        if (rows.isEmpty()) return "";

        // Flatten nested data and save to flatRows
        List<LinkedHashMap<String, String>> flatRows = new ArrayList<>(rows.size());
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        for (ObjectNode row : rows) {
            LinkedHashMap<String, String> flat = new LinkedHashMap<>();
            flatten("", row, arrayDelimiter, flat);
            flatRows.add(flat);
            headers.addAll(flat.keySet()); // union of keys (in encounter order)
        }

        sortHeaders(headers);

        CsvSchema.Builder sb = CsvSchema.builder();
        for (String h : headers) {
            sb.addColumn(h);
        }
        CsvSchema schema = sb
                .setUseHeader(true)
                .setColumnSeparator(delimiter)
                .build();

        // Write csv using flatRows
        CsvMapper csv = new CsvMapper();
        StringWriter out = new StringWriter();
        try (SequenceWriter w = csv.writer(schema).writeValues(out)) {
            for (Map<String, String> r : flatRows) {
                w.write(r);
            }
        }
        return out.toString();
    }

    /**
     * Preserves original order of simple fields and sorts only the complex fields, appending them at the end.
     */
    private static void sortHeaders(@NotNull LinkedHashSet<String> headers) {
        List<String> toSort = new ArrayList<>();
        for (Iterator<String> it = headers.iterator(); it.hasNext(); ) {
            String header = it.next();
            if (header.contains("[")) { // heuristically find complex fields -only flattened fields should contain '[' (e.g. field[0].a)
                toSort.add(header);
                it.remove();
            }
        }
        toSort.sort(Comparator.naturalOrder());
        headers.addAll(toSort);
    }

    /**
     * Ensure we have an array of object nodes to iterate.
     */
    private static List<ObjectNode> normalize(JsonNode root) {
        if (root == null || root.isNull()) return List.of();
        if (root.isArray()) {
            List<ObjectNode> list = new ArrayList<>();
            for (JsonNode n : root) {
                if (n != null && n.isObject()) {
                    list.add((ObjectNode) n);
                } else if (n != null && !n.isNull()) {
                    // If array elements are primitives/arrays, wrap them
                    ObjectNode obj = mapper.createObjectNode();
                    obj.set("value", n.deepCopy());
                    list.add(obj);
                }
            }
            return list;
        } else if (root.isObject()) {
            return List.of((ObjectNode) root);
        } else {
            ObjectNode obj = mapper.createObjectNode();
            obj.set("value", root);
            return List.of(obj);
        }
    }

    /**
     * Flatten objects/arrays into dotted keys suitable for CSV.
     */
    private static void flatten(@NotNull String prefix, JsonNode node, @NotNull Map<String, String> out) {
        flatten(prefix, node, ';', out);
    }

    /**
     * Flatten objects/arrays into dotted keys suitable for CSV.
     */
    private static void flatten(@NotNull String prefix, JsonNode node, char arrayDelimiter, @NotNull Map<String, String> out) {
        if (node == null || node.isNull() || (node.isArray() && node.isEmpty())) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.properties().iterator().forEachRemaining(e -> {
                String key = join(prefix, e.getKey());
                flatten(key, e.getValue(), out);
            });
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            if (allScalars(arr)) {
                // join simple values with arrayDelimiter
                List<String> parts = new ArrayList<>(arr.size());
                for (JsonNode v : arr) parts.add(scalarToString(v));
                out.put(prefix, String.join(String.valueOf(arrayDelimiter), parts));
            } else {
                // index complex items: field[0].a, field[1].b, ...
                for (int i = 0; i < arr.size(); i++) {
                    String key = prefix + "[" + i + "]";
                    flatten(key, arr.get(i), out);
                }
            }
        } else { // scalar
            out.put(prefix, scalarToString(node));
        }
    }

    private static boolean allScalars(@NotNull ArrayNode arr) {
        return StreamSupport.stream(arr.spliterator(), false).allMatch(JsonNode::isValueNode);
    }

    private static String scalarToString(JsonNode n) {
        if (n == null || n.isNull()) return "";
        return n.asText();
    }

    private static String join(String prefix, String key) {
        return prefix == null || prefix.isEmpty() ? key : prefix + "." + key;
    }
}
