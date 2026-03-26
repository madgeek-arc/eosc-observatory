package eu.openaire.observatory.utils;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.InvalidModificationException;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JSONObjectUtilsTest {

    @Test
    void createPathBuildsNestedObjects() {
        JSONObject root = new JSONObject();

        JSONObjectUtils.createPath("survey.section.question", root);

        assertThat(JsonPath.<Object>read(root.toJSONString(), "$.survey.section.question")).isNotNull();
    }

    @Test
    void addSetsNestedScalarValue() {
        JSONObject root = new JSONObject();

        JSONObjectUtils.add("survey.section.answer", "value", root);

        assertThat(JsonPath.<String>read(root.toJSONString(), "$.survey.section.answer")).isEqualTo("value");
    }

    @Test
    void addRejectsArrayPathsWithCurrentImplementation() {
        JSONObject root = new JSONObject();

        assertThatThrownBy(() -> JSONObjectUtils.add("questions[0]", "first", root))
                .isInstanceOf(InvalidModificationException.class);
    }
}
