package eu.openaire.observatory.resources.analyzer.model;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.springframework.http.HttpStatus;

import java.util.List;

public record GenerateDocumentsRequest(String surveyId, List<String> surveyAnswerIds){

    public GenerateDocumentsRequest {
        surveyAnswerIds = (surveyAnswerIds == null) ? List.of() : List.copyOf(surveyAnswerIds);

        boolean hasSurveyId = surveyId != null && !surveyId.isBlank();
        boolean hasAnswers = !surveyAnswerIds.isEmpty();

        if (hasSurveyId == hasAnswers) {
            throw new ResourceException("Provide either surveyId or surveyAnswerIds.", HttpStatus.BAD_REQUEST);
        }
    }
}