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

package eu.openaire.observatory.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "privacy")
public class PrivacyProperties {

    private final List<WrapperClass> wrapperClasses = new ArrayList<>();
    private final List<FieldPrivacy> entries = new ArrayList<>();


    public PrivacyProperties() {
        // no-arg constructor
    }

    public List<WrapperClass> getWrapperClasses() {
        return wrapperClasses;
    }

    public List<FieldPrivacy> getEntries() {
        return entries;
    }

    public static class ClassFields {

        private final Map<String, List<FieldPrivacy>> clazz = new LinkedHashMap<>();

        public ClassFields() {
        }

        public Map<String, List<FieldPrivacy>> getClazz() {
            return clazz;
        }
    }

    public static class WrapperClass {

        private Class<?> clazz;
        private String field;

        public WrapperClass() {
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public void setClazz(Class<?> clazz) {
            this.clazz = clazz;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    public static class FieldPrivacy {

        private String className;
        private String field;
        private Policy policy;

        public FieldPrivacy() {
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public Policy getPolicy() {
            return policy;
        }

        public void setPolicy(Policy policy) {
            this.policy = policy;
        }
    }

    public enum Policy {
        STRICT,
        PUBLIC
    }
}
