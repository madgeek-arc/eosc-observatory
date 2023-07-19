package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.Diff;
import eu.eosc.observatory.dto.HistoryDTO;
import eu.eosc.observatory.dto.SurveyAnswerInfo;
import eu.eosc.observatory.dto.SurveyAnswerMetadataDTO;
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
     * Updates SurveyAnswer object keeping the authenticated user and the time of modification as metadata.
     *
     * @param id
     * @param surveyAnswer
     * @param comment
     * @param authentication
     * @return
     * @throws ResourceNotFoundException
     */
    SurveyAnswer update(String id, SurveyAnswer surveyAnswer, String comment, Authentication authentication) throws ResourceNotFoundException;

    SurveyAnswer updateAnswer(String surveyAnswerId, JSONObject answer, String comment, Authentication authentication) throws ResourceNotFoundException;

    Diff surveyAnswerDiff(String surveyAnswerId, String version1Id, String version2Id);

    SurveyAnswer setAnswerValidated(String surveyAnswerId, boolean validated, Authentication authentication) throws ResourceNotFoundException;

    SurveyAnswer setAnswerPublished(String surveyAnswerId, boolean published, Authentication authentication) throws ResourceNotFoundException;

    List<SurveyAnswer> generateAnswers(String surveyId, Authentication authentication);

    SurveyAnswer generateStakeholderAnswer(String stakeholderId, String surveyId, Authentication authentication);

    Browsing<SurveyAnswerInfo> browseSurveyAnswersInfo(FacetFilter filter);

    Object getValueFromAnswer(UiField field, Map<String, ?> answer, Map<String, UiField> allFields);

    void lockSurveyAndAnswers(String surveyId, boolean lock);

    HistoryDTO getHistory(String surveyAnswerId);

    SurveyAnswerMetadataDTO getPublicMetadata(String surveyAnswerId);

    SurveyAnswer restore(String surveyAnswerId, String versionId);
}
