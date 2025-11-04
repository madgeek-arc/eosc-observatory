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
package eu.openaire.observatory.utils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class JSONObjectUtils {

    private static final Logger logger = LoggerFactory.getLogger(JSONObjectUtils.class);

    /**
     * <p>Generates the path to the required field in the provided JSON Object.
     * If the path to the field already exist it behaves as a noop.</p>
     *
     * @param field the path to the field expressed in JSON Path.
     * @param obj the JSON Object.
     */
    public static void createPath(String field, JSONObject obj) {
        List<String> fields = Arrays.stream(field.split("\\.")).toList();
        StringBuilder path = new StringBuilder();

        Configuration suppressExceptionConfiguration = Configuration
                .defaultConfiguration()
                .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
                .addOptions(Option.SUPPRESS_EXCEPTIONS);

        path.append("$");
        for (String current : fields) {
            String pathToCurrent = String.format("%s.%s", path, current);
            ReadContext jsonData = JsonPath.using(suppressExceptionConfiguration).parse(obj);
            Object data = null;

            data = jsonData.read(pathToCurrent);
            if (data == null) {
                if (current.matches(".*\\[\\d+\\]")) {
                    if (!(JsonPath.parse(obj).read(path.toString()) instanceof JSONArray)) {
                        logger.trace("Altering field '{}' to JSON Array : Previous JSON: {}", path, obj);
                        obj = JsonPath.parse(obj).set(path.toString(), new JSONArray()).json();
                        logger.debug("Altered field '{}' to JSON Array", path);
                        logger.trace("Altered field '{}' to JSON Array : New JSON: {}", path, obj);
                    }
                } else {
                    obj = JsonPath.parse(obj).put(path.toString(), current, new JSONObject()).json();
                    logger.debug("Added field '{}'", pathToCurrent);
                    logger.trace("Added field '{}' to JSON: {}", pathToCurrent, obj);
                }
            }
            path.append(".").append(current);
        }
    }

    /**
     * <p>Adds the given value to a field in the provided JSON Object.
     * If the field is an array, it appends the value to the array.</p>
     * <p>If the path to the field does not exists, it generates it.</p>
     *
     * @param field the path to the field expressed in JSON Path.
     * @param value the value to set.
     * @param obj the JSON Object.
     * @return the JSON Object.
     */
    public static JSONObject add(String field, Object value, JSONObject obj) {
        createPath(field, obj);
        if (field.matches(".*\\[\\d+\\]")) {
            obj = addToArray(field, value, obj);
        } else {
            obj = JsonPath.parse(obj).set(field, value).json();
        }
        return obj;
    }

    /**
     * <p>Appends the given value to the provided array field in the JSON Object.</p>
     *
     * @param field the array field expressed in JSON Path.
     * @param value the value to append.
     * @param obj the JSON Object.
     * @return the JSON Object.
     */
    private static JSONObject addToArray(String field, Object value, JSONObject obj) {
        field = field.substring(0, field.lastIndexOf("."));
        obj = JsonPath/*.using(conf)*/.parse(obj).add(field, value).json();
        return obj;
    }

    private JSONObjectUtils() {
    }
}
