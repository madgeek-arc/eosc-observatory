package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.ui.domain.Survey;
import org.json.simple.JSONObject;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface SurveyService {

    Browsing<Survey> getByType(FacetFilter filter, String type);

    SurveyAnswer getLatest(String surveyId, String stakeholderId);

    List<SurveyAnswer> getActive(String stakeholderId);

    SurveyAnswer updateAnswer(String id, JSONObject answer, User user) throws ResourceNotFoundException;

    List<SurveyAnswer> createNewCycle(Authentication authentication);
}
