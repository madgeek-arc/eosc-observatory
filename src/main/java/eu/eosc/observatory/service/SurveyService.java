package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.catalogue.ui.domain.Survey;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface SurveyService {

    Browsing<Survey> getByType(FacetFilter filter, String type);

    List<SurveyAnswer> createNewCycle(Authentication authentication);
}
