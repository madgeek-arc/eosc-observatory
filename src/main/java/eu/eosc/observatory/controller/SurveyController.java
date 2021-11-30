package eu.eosc.observatory.controller;

import eu.eosc.observatory.service.CrudItemService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

@RestController
@RequestMapping("surveys")
public class SurveyController {

    private static final Logger logger = LogManager.getLogger(SurveyController.class);

    private final GenericItemService genericItemService;

    @Autowired
    public SurveyController(@Qualifier("catalogueGenericItemService") GenericItemService genericItemService) {
        this.genericItemService = genericItemService;
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping("/type/{type}")
    public ResponseEntity<Browsing<Survey>> getSurveysByType(@ApiIgnore @RequestParam Map<String, Object> allRequestParams, @PathVariable("type") String type) {
        allRequestParams.put("resourceType", "survey");
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Browsing<Survey> surveyBrowsing = this.genericItemService.getResults(filter);
        return new ResponseEntity<>(surveyBrowsing, HttpStatus.OK);
    }
}
