package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.Diff;
import eu.eosc.observatory.dto.HistoryDTO;
import eu.eosc.observatory.dto.SurveyAnswerInfo;
import eu.eosc.observatory.dto.SurveyAnswerMetadataDTO;
import eu.eosc.observatory.service.CoordinatorService;
import eu.eosc.observatory.service.CrudService;
import eu.eosc.observatory.service.StakeholderService;
import eu.eosc.observatory.service.SurveyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.ui.controller.FormsController;
import gr.athenarc.catalogue.ui.domain.Model;
import gr.athenarc.catalogue.utils.PagingUtils;
import io.swagger.v3.oas.annotations.Parameter;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class SurveyController {

    private static final Logger logger = LoggerFactory.getLogger(SurveyController.class);

    private final FormsController formsController;
    private final CrudService<SurveyAnswer> surveyAnswerService;
    private final SurveyService surveyService;
    private final StakeholderService stakeholderService;
    private final CoordinatorService coordinatorService;

    @Autowired
    public SurveyController(FormsController formsController,
                            CrudService<SurveyAnswer> surveyAnswerService,
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

    @Browse
    @GetMapping("surveys")
    public ResponseEntity<Browsing<Model>> getSurveysByStakeholderOrType(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams, @RequestParam(value = "stakeholderId", defaultValue = "") String stakeholderId, @RequestParam(value = "type", defaultValue = "") String type) {
        allRequestParams.remove("stakeholderId");
        FacetFilter filter = PagingUtils.createFacetFilter(allRequestParams);
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
    public ResponseEntity<Model> addSurvey(@RequestBody Model survey, @Parameter(hidden = true) Authentication authentication) {
        survey.setCreatedBy(User.getId(authentication));
        survey.setModifiedBy(survey.getCreatedBy());
        Date date = new Date();
        survey.setCreationDate(date);
        survey.setModificationDate(date);
        return formsController.addModel(survey);
    }

    @PutMapping("surveys/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Model> updateSurvey(@PathVariable("id") String id, @RequestBody Model survey, @Parameter(hidden = true) Authentication authentication) {
        survey.setModifiedBy(User.getId(authentication));
        survey.setModificationDate(new Date());
        return formsController.updateModel(id, survey);
    }

    @PostMapping("surveys/{id}/lock")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> lockSurvey(@PathVariable("id") String surveyId,
                                           @RequestParam("lock") Boolean lock,
                                           @Parameter(hidden = true) Authentication authentication) {
        if (lock != null) {
            surveyService.lockSurveyAndAnswers(surveyId, lock);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }


    /*-------------------------------------*/
    /*           Survey Answers            */
    /*-------------------------------------*/

    @PutMapping("answers/{surveyAnswerId}/import/{model}")
    @PreAuthorize("hasPermission(#surveyAnswerId, 'write')")
    public ResponseEntity<SurveyAnswer> importSurveyAnswer(@PathVariable("surveyAnswerId") String surveyAnswerId,
                                                           @PathVariable("model") String modelFrom,
                                                           @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        return new ResponseEntity<>(surveyService.importAnswer(surveyAnswerId, modelFrom, authentication), HttpStatus.OK);
    }

    @PutMapping("answers/{surveyAnswerId}/answer")
    @PreAuthorize("hasPermission(#surveyAnswerId, 'write')")
    public ResponseEntity<SurveyAnswer> updateSurveyAnswer(@PathVariable("surveyAnswerId") String surveyAnswerId,
                                                           @RequestBody JSONObject object,
                                                           @RequestParam(name = "comment", defaultValue = "") String comment,
                                                           @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        return new ResponseEntity<>(surveyService.updateAnswer(surveyAnswerId, object, comment, authentication), HttpStatus.OK);
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
                                                             @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        return new ResponseEntity<>(surveyService.setAnswerValidated(surveyAnswerId, validated, authentication), HttpStatus.OK);
    }

    @PatchMapping("answers/{id}/publish")
    @PreAuthorize("hasPermission(#surveyAnswerId, 'publish')")
    public ResponseEntity<SurveyAnswer> publishAnswer(@PathVariable("id") String surveyAnswerId,
                                                      @RequestParam(value = "published") boolean published,
                                                      @Parameter(hidden = true) Authentication authentication) throws ResourceNotFoundException {
        SurveyAnswer surveyAnswers = surveyService.setAnswerPublished(surveyAnswerId, published, authentication);
        return new ResponseEntity<>(surveyAnswers, HttpStatus.OK);
    }

    @Browse
    @GetMapping("answers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<SurveyAnswer>> getAnswers(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = PagingUtils.createFacetFilter(allRequestParams);
        Browsing<SurveyAnswer> answers = surveyAnswerService.getAll(filter);
        return new ResponseEntity<>(answers, HttpStatus.OK);
    }

    @GetMapping("answers/latest")
    @PostAuthorize("hasPermission(returnObject, 'read') or hasCoordinatorAccess(returnObject) or hasStakeholderManagerAccess(returnObject)")
    public ResponseEntity<SurveyAnswer> getLatest(@RequestParam("surveyId") String surveyId, @RequestParam("stakeholderId") String stakeholderId) throws ResourceNotFoundException {
        SurveyAnswer surveyAnswer = surveyService.getLatest(surveyId, stakeholderId);
        if (surveyAnswer == null) {
            throw new ResourceNotFoundException();
        }
        return new ResponseEntity<>(surveyAnswer, HttpStatus.OK);
    }

    @GetMapping("answers/{id}")
    @PreAuthorize("hasPermission(#id, 'read') or hasCoordinatorAccess(#id) or hasStakeholderManagerAccess(#id)")
    public ResponseEntity<SurveyAnswer> getSurveyAnswer(@PathVariable("id") String id, @Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>(surveyAnswerService.get(id), HttpStatus.OK);
    }

    @GetMapping("answers/{id}/answer")
    @PreAuthorize("hasPermission(#id, 'read') or hasCoordinatorAccess(#id) or hasStakeholderManagerAccess(#id)")
    public ResponseEntity<?> getAnswer(@PathVariable("id") String id, @Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>(surveyAnswerService.get(id).getAnswer(), HttpStatus.OK);
    }

    @GetMapping(value = "answers/{id}/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPublicAnswer(@PathVariable("id") String id) {
        return new ResponseEntity<>(surveyAnswerService.get(id).getAnswer(), HttpStatus.OK);
    }

    @GetMapping(value = "answers/public", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPublicAnswer(@RequestParam("surveyId") String surveyId, @RequestParam("stakeholderId") String stakeholderId) throws ResourceNotFoundException {
        SurveyAnswer surveyAnswer = surveyService.getLatest(surveyId, stakeholderId);
        if (surveyAnswer == null) {
            throw new ResourceNotFoundException();
        }
        return new ResponseEntity<>(surveyAnswer.getAnswer(), HttpStatus.OK);
    }

    @GetMapping(value = "answers/public/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SurveyAnswerMetadataDTO> getPublicAnswerMetadata(@RequestParam("surveyId") String surveyId, @RequestParam("stakeholderId") String stakeholderId) throws ResourceNotFoundException {
        SurveyAnswer surveyAnswer = surveyService.getLatest(surveyId, stakeholderId);
        if (surveyAnswer == null) {
            throw new ResourceNotFoundException();
        }
        SurveyAnswerMetadataDTO metadataDTO = surveyService.getPublicMetadata(surveyAnswer.getId());
        return new ResponseEntity<>(metadataDTO, HttpStatus.OK);
    }

    @PostMapping("answers/generate/{surveyId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<SurveyAnswer>> generateAnswers(@PathVariable("surveyId") String surveyId,
                                                              @RequestParam(value = "stakeholderId", required = false, defaultValue = "") String stakeholderId,
                                                              @Parameter(hidden = true) Authentication authentication) {
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

    @Browse
    @GetMapping("answers/info")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinator(#coordinatorId != null ? #coordinatorId : #groupId) or isStakeholderManager(#stakeholderId != null ? #stakeholderId : #groupId)")
    public ResponseEntity<Browsing<SurveyAnswerInfo>> getSurveyInfo(@RequestParam(value = "groupId", required = false) String groupId,
                                                                    @RequestParam(value = "coordinator", required = false) String coordinatorId,
                                                                    @RequestParam(value = "stakeholder", required = false) String stakeholderId,
                                                                    @Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        allRequestParams.remove("coordinator");
        allRequestParams.remove("stakeholder");
        allRequestParams.remove("groupId");
        FacetFilter filter = PagingUtils.createFacetFilter(allRequestParams);
        String type = null;
        if (coordinatorId != null && stakeholderId != null) {
            throw new UnsupportedOperationException("Only one of ['coordinator', 'stakeholder'] is expected..");
        }
        if (groupId != null) {
            if (groupId.startsWith("co-")) {
                coordinatorId = groupId;
                stakeholderId = null;
            } else if (groupId.startsWith("sh-")) {
                stakeholderId = groupId;
                coordinatorId = null;
            } else {
                throw new UnsupportedOperationException("unrecognized group id");
            }
        }
        if (coordinatorId != null) {
            type = coordinatorService.get(coordinatorId).getType();
        } else if (stakeholderId != null) {
            type = stakeholderService.get(stakeholderId).getType();
        } else {
            throw new UnsupportedOperationException("One of ['coordinator', 'stakeholder'] is expected..");
        }

        filter.addFilter("type", type);
        return new ResponseEntity<>(surveyService.browseSurveyAnswersInfo(filter), HttpStatus.OK);
    }

    @GetMapping("answers/{id}/history")
    @PreAuthorize("hasAuthority('ADMIN') or hasPermission(#id, 'read') or hasCoordinatorAccess(#id) or hasStakeholderManagerAccess(#id)")
    public ResponseEntity<HistoryDTO> history(@PathVariable("id") String id) {
        return new ResponseEntity<>(surveyService.getHistory(id), HttpStatus.OK);
    }

    @GetMapping("answers/{id}/versions/{version}")
    @PreAuthorize("hasAuthority('ADMIN') or hasPermission(#id, 'read') or hasCoordinatorAccess(#id) or hasStakeholderManagerAccess(#id)")
    public ResponseEntity<SurveyAnswer> version(@PathVariable("id") String id, @PathVariable("version") String version) {
        return new ResponseEntity<>(surveyAnswerService.getVersion(id, version), HttpStatus.OK);
    }

    @GetMapping(value = "answers/{id}/diff", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('ADMIN') or hasPermission(#id, 'read') or hasCoordinatorAccess(#id) or hasStakeholderManagerAccess(#id)")
    public ResponseEntity<Diff> diff(@PathVariable("id") String id, @RequestParam("v1") String v1, @RequestParam("v2") String v2) {
        return new ResponseEntity<>(surveyService.surveyAnswerDiff(id, v1, v2), HttpStatus.OK);
    }

    @PutMapping(value = "answers/{id}/versions/{version}/restore", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('ADMIN') or hasPermission(#id, 'write')")
    public ResponseEntity<SurveyAnswer> restoreVersion(@PathVariable("id") String id, @PathVariable("version") String version) {
        return new ResponseEntity<>(surveyService.restore(id, version), HttpStatus.OK);
    }
}
