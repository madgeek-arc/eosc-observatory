package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.SurveyAnswerInfo;
import eu.eosc.observatory.service.CoordinatorService;
import eu.eosc.observatory.service.SurveyService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.controller.GenericItemController;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("coordinators")
public class CoordinatorController {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatorController.class);

    private final CoordinatorService coordinatorService;
    private final SurveyService surveyService;

    @Autowired
    public CoordinatorController(CoordinatorService coordinatorService, SurveyService surveyService) {
        this.coordinatorService = coordinatorService;
        this.surveyService = surveyService;
    }

    /*---------------------------*/
    /*        CRUD methods       */
    /*---------------------------*/

    @GetMapping("{id}")
//    @PreAuthorize("isCoordinatorMember(#id)")
    public ResponseEntity<Coordinator> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(coordinatorService.get(id), HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Coordinator> create(@RequestBody Coordinator coordinator) {
        return new ResponseEntity<>(coordinatorService.add(coordinator), HttpStatus.CREATED);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")// or isCoordinatorManager(#coordinatorId)")
    public ResponseEntity<Coordinator> update(@PathVariable("id") String id, @RequestBody Coordinator coordinator) throws ResourceNotFoundException {
        return new ResponseEntity<>(coordinatorService.update(id, coordinator), HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Coordinator> delete(@PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(coordinatorService.delete(id), HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<Coordinator>> getCoordinators(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Browsing<Coordinator> coordinators = coordinatorService.getAll(filter);
        return new ResponseEntity<>(coordinators, HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Member methods      */
    /*---------------------------*/

    @GetMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")// or isCoordinatorMember(#coordinatorId)")
    public ResponseEntity<Set<User>> getMembers(@PathVariable("id") String coordinatorId) {
        return new ResponseEntity<>(coordinatorService.getMembers(coordinatorId), HttpStatus.OK);
    }

    @PatchMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<User>> updateMembers(@PathVariable("id") String coordinatorId, @RequestBody List<String> emails) {
        return new ResponseEntity<>(coordinatorService.updateMembers(coordinatorId, emails), HttpStatus.OK);
    }


    @PostMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<User>> addMember(@PathVariable("id") String coordinatorId, @RequestBody String email) {
        return new ResponseEntity<>(coordinatorService.addMember(coordinatorId, email), HttpStatus.OK);
    }

    @DeleteMapping("{id}/members/{contributorId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<User>> removeMember(@PathVariable("id") String coordinatorId, @PathVariable("contributorId") String contributorId) {
        return new ResponseEntity<>(coordinatorService.removeMember(coordinatorId, contributorId), HttpStatus.OK);
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
    @GetMapping("{id}/surveys")
    public ResponseEntity<Browsing<SurveyAnswerInfo>> getSurveyInfo(@PathVariable("id") String coordinatorId,
                                                                    @ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Coordinator coordinator = coordinatorService.get(coordinatorId);
        return new ResponseEntity<>(surveyService.browseSurveyAnswersInfo(coordinator.getType(), filter), HttpStatus.OK);
    }


}

