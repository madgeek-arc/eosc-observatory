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
package eu.eosc.observatory.permissions;

import java.util.Arrays;

public enum Permissions {
    READ("read"),
    WRITE("write"),
    MANAGE("manage"),
    PUBLISH("publish");

    private final String typeValue;

    Permissions(final String type) {
        this.typeValue = type;
    }

    public String getKey() {
        return typeValue;
    }

    /**
     * @return the Enum representation for the given string.
     * @throws IllegalArgumentException if unknown string.
     */
    public static Permissions fromString(String s) throws IllegalArgumentException {
        return Arrays.stream(Permissions.values())
                .filter(v -> v.typeValue.equalsIgnoreCase(s))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown value: " + s));
    }

    /**
     * Checks if the given {@link String} exists in the values of the enum.
     *
     * @return boolean
     */
    public static boolean exists(String s) {
        return Arrays.stream(Permissions.values())
                .anyMatch(v -> v.typeValue.equalsIgnoreCase(s));
    }
}
