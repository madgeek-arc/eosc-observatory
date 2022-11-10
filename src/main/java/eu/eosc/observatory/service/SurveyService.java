package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.SurveyAnswerInfo;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.ui.domain.Model;
import gr.athenarc.catalogue.ui.domain.UiField;
import org.json.simple.JSONObject;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface SurveyService {

    Browsing<Model> getByType(FacetFilter filter, String type);

    Browsing<Model> getByStakeholder(FacetFilter filter, String stakeholderId);

    SurveyAnswer getLatest(String surveyId, String stakeholderId);

    List<SurveyAnswer> getActive(String stakeholderId);

    List<SurveyAnswer> getAllByStakeholder(String id);

    /**
     * Updates SurveyAnswer object keeping the User and the time of modification as metadata.
     *
     * @param id
     * @param surveyAnswer
     * @param user
     * @return
     * @throws ResourceNotFoundException
     */
    SurveyAnswer update(String id, SurveyAnswer surveyAnswer, User user) throws ResourceNotFoundException;

    SurveyAnswer updateAnswer(String surveyAnswerId, JSONObject answer, User user) throws ResourceNotFoundException;

    SurveyAnswer setAnswerValidated(String surveyAnswerId, boolean validated, User user) throws ResourceNotFoundException;

    SurveyAnswer setAnswerPublished(String surveyAnswerId, boolean published, User user) throws ResourceNotFoundException;

    List<SurveyAnswer> generateAnswers(String surveyId, Authentication authentication);

    SurveyAnswer generateStakeholderAnswer(String stakeholderId, String surveyId, Authentication authentication);

    Browsing<SurveyAnswerInfo> browseSurveyAnswersInfo(FacetFilter filter);

    Browsing<SurveyAnswerInfo> browseSurveyAnswersInfo(String type, FacetFilter filter);

    Object getValueFromAnswer(UiField field, JSONObject answer, Map<String, UiField> allFields);

    void lockSurveyAndAnswers(String surveyId, boolean lock);
}
