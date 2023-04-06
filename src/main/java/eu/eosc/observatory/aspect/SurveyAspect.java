package eu.eosc.observatory.aspect;

import eu.eosc.observatory.domain.PrivacyPolicy;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.SurveyAnswerCrudService;
import eu.eosc.observatory.service.SurveyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.ui.domain.Model;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.flywaydb.core.internal.strategy.RetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Aspect
@Component
public class SurveyAspect {

    private static final Logger logger = LoggerFactory.getLogger(SurveyAspect.class);

    private final SurveyService surveyService;
    private final SurveyAnswerCrudService surveyAnswerService;


    public SurveyAspect(SurveyService surveyService, SurveyAnswerCrudService surveyAnswerService) {
        this.surveyService = surveyService;
        this.surveyAnswerService = surveyAnswerService;
    }


    @Retryable
    @AfterReturning(value = "execution (* eu.eosc.observatory.service.StakeholderService.add(..))", returning = "stakeholder")
    void generateStakeholderAnswers(Stakeholder stakeholder) {
        logger.info("Generating Stakeholder Surveys");
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(10_000);
        Browsing<Model> models = surveyService.getByType(ff, stakeholder.getType());
        for (Model model : models.getResults()) {
            surveyService.generateStakeholderAnswer(
                    stakeholder.getId(),
                    model.getId(),
                    SecurityContextHolder.getContext().getAuthentication());
        }
    }


    @Retryable
    @Around("execution (* eu.eosc.observatory.service.StakeholderService.delete(String)) && args(stakeholderId)")
    Object purgeStakeholderRelatedData(ProceedingJoinPoint pjp, String stakeholderId) {
        logger.info("Removing Stakeholder Data");
        Stakeholder stakeholder;
        try {
            List<SurveyAnswer> surveyAnswers = surveyService.getAllByStakeholder(stakeholderId);
            surveyAnswers.forEach(answer -> surveyAnswerService.delete(answer.getId()));
            stakeholder = (Stakeholder) pjp.proceed();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            logger.error("proceed() failed..", e);
            throw new RuntimeException(e);
        }
        return stakeholder;
    }


    @Retryable
    @AfterReturning("execution (* gr.athenarc.catalogue.ui.service.ModelService.delete(String)) && args(id)")
    void purgeSurveyRelatedData(String id) {
        logger.info("Removing all associated Survey Answers and User permissions");
        Set<SurveyAnswer> answerSet = surveyAnswerService.getWithFilter("surveyId", id);
        // used SurveyAnswerCrudService to also delete user permissions (through PermissionsUpdateAspect)
        answerSet.forEach(answer -> surveyAnswerService.delete(answer.getId()));
    }

}
