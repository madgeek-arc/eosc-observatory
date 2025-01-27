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
package eu.eosc.observatory.domain;

import java.io.Serializable;

public class Action implements Serializable {

    Type type = Type.UPDATE;
    Integer index;

    public Action() {
    }

    public Action(Type type, Integer index) {
        this.type = type;
        this.index = index;
    }

    public Type getType() {
        return type;
    }

    public Action setType(Type type) {
        this.type = type;
        return this;
    }

    public Integer getIndex() {
        return index;
    }

    public Action setIndex(Integer index) {
        this.index = index;
        return this;
    }

    public enum Type {
        UPDATE,
        ADD,
        MOVE,
        DELETE
    }

}
