package eu.eosc.observatory.utils;

import eu.eosc.observatory.domain.SurveyAnswer;

import java.util.*;
import java.util.stream.Collectors;

public class SurveyAnswerUtils {

    public static List<String> getSurveyAnswerIds(Collection<SurveyAnswer> answers) {
        if (answers == null) {
            return Collections.emptyList();
        }
        Set<String> resourceIds = answers.stream().filter(Objects::nonNull).map(SurveyAnswer::getId).collect(Collectors.toSet());
        return new ArrayList<>(resourceIds);
    }

    private SurveyAnswerUtils() {}
}
