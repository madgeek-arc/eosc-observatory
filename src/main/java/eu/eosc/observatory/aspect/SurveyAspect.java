package eu.eosc.observatory.aspect;

import eu.eosc.observatory.domain.PrivacyPolicy;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.SurveyAnswerCrudService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Aspect
@Component
public class SurveyAspect {

    private static final Logger logger = LoggerFactory.getLogger(SurveyAspect.class);

    private final SurveyAnswerCrudService surveyAnswerService;


    public SurveyAspect(SurveyAnswerCrudService surveyAnswerService) {
        this.surveyAnswerService = surveyAnswerService;
    }

    @Around("execution (* gr.athenarc.catalogue.ui.service.ModelService.delete(String)) && args(id)")
    void purgeSurveyRelatedData(ProceedingJoinPoint pjp, String id) {

        try {
            Set<SurveyAnswer> answerSet = surveyAnswerService.getWithFilter("surveyId", id);
            // used SurveyAnswerCrudService to delete user permissions (through PermissionsUpdateAspect)
            answerSet.forEach(answer -> surveyAnswerService.delete(answer.getId()));

            pjp.proceed();
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
        } catch (Throwable e) {
            logger.error("proceed() failed..", e);
            throw new RuntimeException(e);
        }
    }

}
