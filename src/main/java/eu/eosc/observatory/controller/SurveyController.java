package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.SurveyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
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
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping("surveys/type/{type}")
    public ResponseEntity<Browsing<Survey>> getSurveysByType(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @PathVariable("type") String type) {
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Browsing<Survey> surveyBrowsing = surveyService.getByType(filter, type);
        return new ResponseEntity<>(surveyBrowsing, HttpStatus.OK);
    }


    /*-------------------------------------*/
    /*           Survey Answers            */
    /*-------------------------------------*/

    @PutMapping("answers/{id}")
    @PreAuthorize("hasPermission(#id, 'write')")
    public ResponseEntity<SurveyAnswer> addSurveyAnswer(@PathVariable("id") String id,
                                                        @RequestBody JSONObject object,
                                                        @ApiIgnore Authentication authentication) {
        SurveyAnswer surveyAnswer = surveyAnswerService.get(id);
        // FIXME: replace add method with update answer method and update survey metadata
        return new ResponseEntity<>(surveyAnswerService.add(surveyAnswer), HttpStatus.CREATED);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping("answers")
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
}
