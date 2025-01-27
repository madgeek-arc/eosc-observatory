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
package eu.eosc.observatory.dto;

import java.util.LinkedList;
import java.util.List;

public class Node {

    String name;
    Modification modification = null;
    List<Node> fields = new LinkedList<>();

    public Node() {
    }

    public Node(String name, Modification modification, List<Node> fields) {
        this.name = name;
        this.modification = modification;
        this.fields = fields;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Modification getModification() {
        return modification;
    }

    public void setModification(Modification modification) {
        this.modification = modification;
    }

    public List<Node> getFields() {
        return fields;
    }

    public void setFields(List<Node> fields) {
        this.fields = fields;
    }
}
