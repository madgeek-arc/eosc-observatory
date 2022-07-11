package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.SurveyAnswerInfo;
import eu.eosc.observatory.service.CoordinatorService;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.StakeholderService;
import eu.eosc.observatory.service.SurveyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.controller.GenericItemController;
import gr.athenarc.catalogue.ui.controller.FormsController;
import gr.athenarc.catalogue.ui.domain.Model;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class SurveyController {

    private static final Logger logger = LoggerFactory.getLogger(SurveyController.class);

    private final FormsController formsController;
    private final CrudItemService<SurveyAnswer> surveyAnswerService;
    private final SurveyService surveyService;
    private final StakeholderService stakeholderService;
    private final CoordinatorService coordinatorService;

    @Autowired
    public SurveyController(FormsController formsController,
                            CrudItemService<SurveyAnswer> surveyAnswerService,
                            SurveyService surveyService,
                            StakeholderService stakeholderService,
                            CoordinatorService coordinatorService) {
        this.formsController = formsController;
        this.surveyAnswerService = surveyAnswerService;
        this.surveyService = surveyService;
        this.stakeholderService = stakeholderService;
        this.coordinatorService = coordinatorService;
    }

    /*-------------------------------------*/
    /*               Surveys               */
    /*-------------------------------------*/

    @GetMapping("surveys/{id}")
//    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Model> getSurvey(@PathVariable("id") String id) {
        return formsController.getModel(id);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping("surveys")
    public ResponseEntity<Browsing<Model>> getSurveysByStakeholderOrType(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @RequestParam(value = "stakeholderId", defaultValue = "") String stakeholderId, @RequestParam(value = "type", defaultValue = "") String type) {
        allRequestParams.remove("stakeholderId");
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Browsing<Model> surveyBrowsing;
        if (stakeholderId != null && !"".equals(stakeholderId)) {
            surveyBrowsing = surveyService.getByStakeholder(filter, stakeholderId);
        } else {
            surveyBrowsing = surveyService.getByType(filter, type);
        }
        return new ResponseEntity<>(surveyBrowsing, HttpStatus.OK);
    }

    @PostMapping("surveys")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Model> addSurvey(@RequestBody Model survey, @ApiIgnore Authentication authentication) {
        survey.setCreatedBy(User.of(authentication).getId());
        survey.setModifiedBy(survey.getCreatedBy());
        Date date = new Date();
        survey.setCreationDate(date);
        survey.setModificationDate(date);
        return formsController.addModel(survey);
    }

    @PutMapping("surveys/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Model> updateSurvey(@PathVariable("id") String id, @RequestBody Model survey, @ApiIgnore Authentication authentication) {
        survey.setModifiedBy(User.of(authentication).getId());
        survey.setModificationDate(new Date());
        return formsController.updateModel(id, survey);
    }


    /*-------------------------------------*/
    /*           Survey Answers            */
    /*-------------------------------------*/

    @PutMapping("answers/{surveyAnswerId}")
    @PreAuthorize("hasPermission(#surveyAnswerId, 'write')")
    public ResponseEntity<SurveyAnswer> updateSurveyAnswer(@PathVariable("surveyAnswerId") String surveyAnswerId,
                                                           @RequestBody JSONObject object,
                                                           @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        return new ResponseEntity<>(surveyService.updateAnswer(surveyAnswerId, object, User.of(authentication)), HttpStatus.OK);
    }

    @DeleteMapping("answers/{surveyAnswerId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<SurveyAnswer> deleteSurveyAnswer(@PathVariable("surveyAnswerId") String surveyAnswerId) throws ResourceNotFoundException {
        return new ResponseEntity<>(surveyAnswerService.delete(surveyAnswerId), HttpStatus.OK);
    }

    @PatchMapping("answers/{id}/validation")
    @PreAuthorize("hasPermission(#surveyAnswerId, 'manage')")
    public ResponseEntity<SurveyAnswer> validateSurveyAnswer(@PathVariable("id") String surveyAnswerId,
                                                             @RequestParam(value = "validated") boolean validated,
                                                             @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        return new ResponseEntity<>(surveyService.setAnswerValidated(surveyAnswerId, validated, User.of(authentication)), HttpStatus.OK);
    }

    @PatchMapping ("answers/{id}/publish")
    @PreAuthorize("hasPermission(#surveyAnswerId, 'publish')")
    public ResponseEntity<SurveyAnswer> publishAnswer(@PathVariable("id") String surveyAnswerId,
                                                      @RequestParam(value = "published") boolean published,
                                                      @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        SurveyAnswer surveyAnswers = surveyService.setAnswerPublished(surveyAnswerId, published, User.of(authentication));
        return new ResponseEntity<>(surveyAnswers, HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping("answers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<SurveyAnswer>> getAnswers(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Browsing<SurveyAnswer> answers = surveyAnswerService.getAll(filter);
        return new ResponseEntity<>(answers, HttpStatus.OK);
    }

    @GetMapping("answers/latest")
    @PostAuthorize("hasPermission(returnObject, 'read') or hasCoordinatorAccess(returnObject) or hasStakeholderManagerAccess(returnObject)")
    public ResponseEntity<SurveyAnswer> getLatest(@RequestParam("surveyId") String surveyId, @RequestParam("stakeholderId") String stakeholderId) {
        SurveyAnswer surveyAnswer = surveyService.getLatest(surveyId, stakeholderId);
        return new ResponseEntity<>(surveyAnswer, HttpStatus.OK);
    }

    @GetMapping("answers/{id}")
    @PreAuthorize("hasPermission(#id, 'read') or hasCoordinatorAccess(#id) or hasStakeholderManagerAccess(#id)")
    public ResponseEntity<SurveyAnswer> getSurveyAnswer(@PathVariable("id") String id, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(surveyAnswerService.get(id), HttpStatus.OK);
    }

    @GetMapping("answers/{id}/answer")
    @PreAuthorize("hasPermission(#id, 'read') or hasCoordinatorAccess(#id) or hasStakeholderManagerAccess(#id)")
    public ResponseEntity<Object> getAnswer(@PathVariable("id") String id, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(surveyAnswerService.get(id).getAnswer(), HttpStatus.OK);
    }

    @PostMapping("answers/generate/{surveyId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<SurveyAnswer>> generateAnswers(@PathVariable("surveyId") String surveyId,
                                                              @RequestParam(value = "stakeholderId", required = false, defaultValue = "") String stakeholderId,
                                                              @ApiIgnore Authentication authentication) {
        List<SurveyAnswer> answers;
        if (stakeholderId != null && !"".equals(stakeholderId)) {
            answers = Collections.singletonList(surveyService.generateStakeholderAnswer(stakeholderId, surveyId, authentication));
        } else {
            answers = surveyService.generateAnswers(surveyId, authentication);
        }
        return new ResponseEntity<>(answers, HttpStatus.CREATED);
    }

    /*---------------------------*/
    /*       Other methods       */
    /*---------------------------*/

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping("answers/info")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorMember(#coordinatorId) or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<Browsing<SurveyAnswerInfo>> getSurveyInfo(@RequestParam(value = "coordinator", required = false) String coordinatorId,
                                                                    @RequestParam(value = "stakeholder", required = false) String stakeholderId,
                                                                    @ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        allRequestParams.remove("coordinator");
        allRequestParams.remove("stakeholder");
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        String type = null;
        if (coordinatorId != null && stakeholderId != null) {
            throw new UnsupportedOperationException("Only one of 'coordinator', 'stakeholder' is expected..");
        }
        if (coordinatorId != null) {
            type = coordinatorService.get(coordinatorId).getType();
        } else {
            type = stakeholderService.get(stakeholderId).getType();
        }

        return new ResponseEntity<>(surveyService.browseSurveyAnswersInfo(type, filter), HttpStatus.OK);
    }
}
