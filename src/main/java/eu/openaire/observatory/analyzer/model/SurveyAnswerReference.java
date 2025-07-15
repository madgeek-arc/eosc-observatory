package eu.openaire.observatory.analyzer.model;

import java.util.List;

public record SurveyAnswerReference(String surveyAnswerId, List<String> fields) implements Reference {
}
