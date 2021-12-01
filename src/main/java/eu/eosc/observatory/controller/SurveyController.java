package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Metadata;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.service.CrudItemService;
import eu.eosc.observatory.service.PermissionsService;
import eu.eosc.observatory.service.SurveyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.catalogue.controller.GenericItemController;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.athenarc.catalogue.ui.domain.Survey;
import gr.athenarc.catalogue.ui.service.FormsService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("answers/{id}")
    public ResponseEntity<SurveyAnswer> addSurveyAnswer(@PathVariable("id") String id,
                                                         @RequestBody JSONObject object,
                                                         @ApiIgnore Authentication authentication) {
        SurveyAnswer surveyAnswer = surveyAnswerService.get(id);
        return new ResponseEntity<>(surveyAnswerService.add(surveyAnswer), HttpStatus.CREATED);
    }

    @PostMapping("cycle/generate")
    public ResponseEntity<List<SurveyAnswer>> generateCycle(@ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(surveyService.createNewCycle(authentication), HttpStatus.CREATED);
    }
}
