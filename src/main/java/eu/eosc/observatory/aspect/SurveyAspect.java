package eu.eosc.observatory.aspect;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.permissions.Groups;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.permissions.Permissions;
import eu.eosc.observatory.service.StakeholderService;
import eu.eosc.observatory.service.SurveyAnswerCrudService;
import eu.eosc.observatory.service.SurveyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.ui.domain.Model;
import gr.athenarc.catalogue.ui.service.ModelService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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

    private final ModelService modelService;
    private final SurveyService surveyService;
    private final PermissionService permissionService;
    private final StakeholderService stakeholderService;
    private final SurveyAnswerCrudService surveyAnswerService;


    public SurveyAspect(ModelService modelService,
                        SurveyService surveyService,
                        PermissionService permissionService,
                        StakeholderService stakeholderService,
                        SurveyAnswerCrudService surveyAnswerService) {
        this.modelService = modelService;
        this.surveyService = surveyService;
        this.permissionService = permissionService;
        this.stakeholderService = stakeholderService;
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


    /**
     * <p>Aspect responsible for updating {@link Stakeholder Stakeholder} admins' and members' {@link Permissions permissions}
     * whenever updating the {@link Model Survey's} {@link Model#active active} field.</p>
     * <p>When the survey becomes inactive, all stakeholder write permissions are removed.</p>
     * <p>When the survey becomes active, all stakeholder write permissions are restored.</p>
     *
     * @param pjp
     * @param id
     * @param model
     * @return
     * @throws Throwable
     */
    @Retryable
    @Around(value = "execution (* gr.athenarc.catalogue.ui.service.ModelService.update(String, gr.athenarc.catalogue.ui.domain.Model)) && args(id, model)", argNames = "pjp,id,model")
    Model managePermissionsWhenActivatingDeactivatingSurvey(ProceedingJoinPoint pjp, String id, Model model) throws Throwable {
        logger.info("Adding/Removing User permissions");

        Model existing = modelService.get(id);
        if (existing.isActive() != model.isActive()) {

            Set<SurveyAnswer> answers = surveyAnswerService.getWithFilter("surveyId", id);
            for (SurveyAnswer answer : answers) {
                if (!model.isActive()) {
                    logger.debug("Removing [write/manage/publish] permissions [stakeholder={}]", answer.getStakeholderId());
                    // model becomes inactive -> remove write permissions
                    List<String> writePermissions = List.of(Permissions.WRITE.getKey(), Permissions.MANAGE.getKey(), Permissions.PUBLISH.getKey());

                    Stakeholder stakeholder = stakeholderService.get(answer.getStakeholderId());
                    permissionService.removePermissions(stakeholder.getUsers(), writePermissions, List.of(answer.getId()), Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
                    permissionService.removePermissions(stakeholder.getUsers(), writePermissions, List.of(answer.getId()), Groups.STAKEHOLDER_MANAGER.getKey());
                } else {
                    logger.debug("Restoring [write/manage/publish] permissions [stakeholder={}]", answer.getStakeholderId());
                    // model becomes active -> restore write permissions
                    Stakeholder stakeholder = stakeholderService.get(answer.getStakeholderId());

                    if (answer.isValidated()) {
                        // add only admin permissions
                        permissionService.addPermissions(stakeholder.getAdmins(), List.of(Permissions.MANAGE.getKey(), Permissions.PUBLISH.getKey()), List.of(answer.getId()), Groups.STAKEHOLDER_MANAGER.getKey());
                    } else {
                        // add member permissions
                        permissionService.addPermissions(
                                stakeholder.getMembers(),
                                List.of(Permissions.WRITE.getKey()),
                                List.of(answer.getId()),
                                Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
                        // add admin permissions
                        permissionService.addPermissions(
                                stakeholder.getAdmins(),
                                List.of(Permissions.WRITE.getKey(), Permissions.MANAGE.getKey(), Permissions.PUBLISH.getKey()),
                                List.of(answer.getId()),
                                Groups.STAKEHOLDER_MANAGER.getKey());
                    }
                }
            }
        }
        return (Model) pjp.proceed();
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
