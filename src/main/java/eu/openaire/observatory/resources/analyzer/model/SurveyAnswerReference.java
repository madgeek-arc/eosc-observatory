package eu.openaire.observatory.resources.analyzer.model;

import java.util.List;

public record SurveyAnswerReference(String surveyAnswerId, List<String> fields) implements Reference {
}
