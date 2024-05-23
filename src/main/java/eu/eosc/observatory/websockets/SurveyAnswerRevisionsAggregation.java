package eu.eosc.observatory.websockets;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import eu.eosc.observatory.domain.SurveyAnswer;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SurveyAnswerRevisionsAggregation implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(SurveyAnswerRevisionsAggregation.class);
    private SurveyAnswer surveyAnswer;
    private List<Revision> revisions;

//    private final Configuration conf = Configuration.defaultConfiguration()
//            .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
////            .addOptions(Option.SUPPRESS_EXCEPTIONS);

    public SurveyAnswerRevisionsAggregation(SurveyAnswer surveyAnswer) {
        this.surveyAnswer = surveyAnswer;
        this.revisions = new ArrayList<>();
    }

    public void applyRevision(Revision revision) {
        JSONObject ans = this.getSurveyAnswer().getAnswer();
        try {
            ans = JsonPath/*.using(conf)*/.parse(ans).set(revision.getField(), revision.getValue()).json();
        } catch (PathNotFoundException e) {
            if (revision.getField().matches(".*\\[\\d+\\]")) {
                ans = addToArray(revision, ans);
            }
            logger.error(e.getMessage(), e);
        }
        this.getSurveyAnswer().setAnswer(ans);
        this.getRevisions().add(revision);
    }

    private JSONObject addToArray(Revision revision, JSONObject obj) {
        String field = revision.getField().substring(0, revision.getField().lastIndexOf("."));
        obj = JsonPath/*.using(conf)*/.parse(obj).add(field, revision.getValue()).json();
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
