package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.SurveyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.controller.GenericItemController;
import gr.athenarc.catalogue.ui.domain.Survey;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class SurveyController {

    private static final Logger logger = LogManager.getLogger(SurveyController.class);

    private final CrudItemService<SurveyAnswer> surveyAnswerService;
    private final SurveyService surveyService;

    @Autowired
    public SurveyController(CrudItemService<SurveyAnswer> surveyAnswerService,
                            SurveyService surveyService) {
        this.surveyAnswerService = surveyAnswerService;
        this.surveyService = surveyService;
    }

    /*-------------------------------------*/
    /*               Surveys               */
    /*-------------------------------------*/

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping("surveys")
    public ResponseEntity<Browsing<Survey>> getSurveysByType(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @RequestParam(value = "type", defaultValue = "") String type) {
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Browsing<Survey> surveyBrowsing = surveyService.getByType(filter, type);
        return new ResponseEntity<>(surveyBrowsing, HttpStatus.OK);
    }


    /*-------------------------------------*/
    /*           Survey Answers            */
    /*-------------------------------------*/

    @PutMapping("answers/{id}")
    @PreAuthorize("hasPermission(#id, 'write')")
    public ResponseEntity<SurveyAnswer> updateSurveyAnswer(@PathVariable("id") String id,
                                                        @RequestBody JSONObject object,
                                                        @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        return new ResponseEntity<>(surveyService.updateAnswer(id, object, User.of(authentication)), HttpStatus.OK);
    }

    @PatchMapping("answers/{id}/validation")
    @PreAuthorize("hasPermission(#id, 'manage')")
    public ResponseEntity<SurveyAnswer> validateSurveyAnswer(@PathVariable("id") String id,
                                                             @RequestParam(value = "validated") boolean validated,
                                                        @ApiIgnore Authentication authentication) throws ResourceNotFoundException {
        return new ResponseEntity<>(surveyService.setAnswerValidated(id, validated, User.of(authentication)), HttpStatus.OK);
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
    @PostAuthorize("hasPermission(returnObject, 'read')")
    public ResponseEntity<SurveyAnswer> getLatest(@RequestParam("surveyId") String surveyId, @RequestParam("stakeholderId") String stakeholderId) {
        SurveyAnswer surveyAnswer = surveyService.getLatest(surveyId, stakeholderId);
        return new ResponseEntity<>(surveyAnswer, HttpStatus.OK);
    }

    @GetMapping("answers/{id}")
    @PreAuthorize("hasPermission(#id, 'read')")
    public ResponseEntity<SurveyAnswer> getSurveyAnswer(@PathVariable("id") String id, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(surveyAnswerService.get(id), HttpStatus.OK);
    }

    @GetMapping("answers/{id}/answer")
    @PreAuthorize("hasPermission(#id, 'read')")
    public ResponseEntity<Object> getAnswer(@PathVariable("id") String id, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(surveyAnswerService.get(id).getAnswer(), HttpStatus.OK);
    }

    @PostMapping("cycle/generate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<SurveyAnswer>> generateCycle(@ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(surveyService.createNewCycle(authentication), HttpStatus.CREATED);
    }

    @PostMapping("answers/generate/{surveyId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<SurveyAnswer>> generateAnswers(@PathVariable("surveyId") String surveyId, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(surveyService.generateAnswers(surveyId, authentication), HttpStatus.CREATED);
    }
}
