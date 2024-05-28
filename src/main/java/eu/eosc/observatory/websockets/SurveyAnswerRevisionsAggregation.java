package eu.eosc.observatory.websockets;

import com.jayway.jsonpath.*;
import eu.eosc.observatory.domain.SurveyAnswer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class SurveyAnswerRevisionsAggregation implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(SurveyAnswerRevisionsAggregation.class);
    private SurveyAnswer surveyAnswer;
    private List<Revision> revisions;

//    private static final Configuration conf = Configuration.defaultConfiguration()
//            .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

    public SurveyAnswerRevisionsAggregation(SurveyAnswer surveyAnswer) {
        this.surveyAnswer = surveyAnswer;
        this.revisions = new ArrayList<>();
    }

    public void applyRevision(Revision revision) {
        JSONObject ans = this.getSurveyAnswer().getAnswer();
        try {
            ans = JsonPath/*.using(conf)*/.parse(ans).set(revision.getField(), revision.getValue()).json();
        } catch (PathNotFoundException e) {
            ans = add(revision.getField(), revision.getValue(), ans);
        }
        this.getSurveyAnswer().setAnswer(ans);
        this.getRevisions().add(revision);
    }

    /**
     * <p>Generates the path to the required field in the provided JSON Object.
     * If the path to the field already exist it behaves as a noop.</p>
     *
     * @param field the path to the field expressed in JSON Path.
     * @param obj the JSON Object.
     */
    public void createPath(String field, JSONObject obj) {
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
    public JSONObject add(String field, Object value, JSONObject obj) {
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
    private JSONObject addToArray(String field, Object value, JSONObject obj) {
        field = field.substring(0, field.lastIndexOf("."));
        obj = JsonPath/*.using(conf)*/.parse(obj).add(field, value).json();
        return obj;
    }

    public SurveyAnswer getSurveyAnswer() {
        return surveyAnswer;
    }

    public void setSurveyAnswer(SurveyAnswer surveyAnswer) {
        this.surveyAnswer = surveyAnswer;
    }

    public List<Revision> getRevisions() {
        return revisions;
    }

    public void setRevisions(List<Revision> revisions) {
        this.revisions = revisions;
    }
}
