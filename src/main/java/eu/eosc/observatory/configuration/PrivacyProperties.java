package eu.eosc.observatory.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "privacy")
public class PrivacyProperties {

    private final List<FieldPrivacy> entries = new ArrayList<>();


    public PrivacyProperties() {
        // no-arg constructor
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
