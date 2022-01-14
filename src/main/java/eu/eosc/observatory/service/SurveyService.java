package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.SurveyAnswerInfo;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.ui.domain.Survey;
import org.json.simple.JSONObject;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface SurveyService {

    Browsing<Survey> getByType(FacetFilter filter, String type);

    Browsing<Survey> getByStakeholder(FacetFilter filter, String stakeholderId);

    SurveyAnswer getLatest(String surveyId, String stakeholderId);

    List<SurveyAnswer> getActive(String stakeholderId);

    List<SurveyAnswer> getAllByStakeholder(String id);

    /**
     * Updates SurveyAnswer object keeping the User and the time of modification as metadata.
     * @param id
     * @param surveyAnswer
     * @param user
     * @return
     * @throws ResourceNotFoundException
     */
    SurveyAnswer update(String id, SurveyAnswer surveyAnswer, User user) throws ResourceNotFoundException;

    SurveyAnswer updateAnswer(String surveyAnswerId, String chapterAnswerId, JSONObject answer, User user) throws ResourceNotFoundException;

    SurveyAnswer setAnswerValidated(String surveyAnswerId, boolean validated, User user) throws ResourceNotFoundException;

    SurveyAnswer setAnswerPublished(String surveyAnswerId, boolean published, User user) throws ResourceNotFoundException;

    @Deprecated
    List<SurveyAnswer> createNewCycle(Authentication authentication);

    List<SurveyAnswer> generateAnswers(String surveyId, Authentication authentication);

    Browsing<SurveyAnswerInfo> browseSurveyAnswersInfo(FacetFilter filter);

    String generateChapterAnswerId();
}
