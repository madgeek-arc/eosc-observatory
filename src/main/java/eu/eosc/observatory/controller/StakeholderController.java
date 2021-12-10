package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.dto.StakeholderMembers;
import eu.eosc.observatory.service.StakeholderService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.controller.GenericItemController;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("stakeholders")
public class StakeholderController {

    private static final Logger logger = LogManager.getLogger(StakeholderController.class);

    private final StakeholderService stakeholderService;

    @Autowired
    public StakeholderController(StakeholderService stakeholderService) {
        this.stakeholderService = stakeholderService;
    }

    /*---------------------------*/
    /*        CRUD methods       */
    /*---------------------------*/

    @GetMapping("{id}")
//    @PreAuthorize("isStakeholderMember(#id)")
    public ResponseEntity<Stakeholder> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(stakeholderService.get(id), HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Stakeholder> create(@RequestBody Stakeholder stakeholder) {
        return new ResponseEntity<>(stakeholderService.add(stakeholder), HttpStatus.CREATED);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")// or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<Stakeholder> update(@PathVariable("id") String id, @RequestBody Stakeholder stakeholder) throws ResourceNotFoundException {
        return new ResponseEntity<>(stakeholderService.update(id, stakeholder), HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Stakeholder> delete(@PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(stakeholderService.delete(id), HttpStatus.OK);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<Stakeholder>> getStakeholders(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Browsing<Stakeholder> stakeholders = stakeholderService.getAll(filter);
        return new ResponseEntity<>(stakeholders, HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Other methods       */
    /*---------------------------*/

    @GetMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")// or isStakeholderMember(#stakeholderId)")
    public ResponseEntity<StakeholderMembers> getMembers(@PathVariable("id") String stakeholderId) {
        return new ResponseEntity<>(stakeholderService.getMembers(stakeholderId), HttpStatus.OK);
    }

    @PatchMapping("{id}/contributors")
    @PreAuthorize("hasAuthority('ADMIN')")// or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<Stakeholder> updateContributors(@PathVariable("id") String stakeholderId, @RequestBody List<String> emails) {
        return new ResponseEntity<>(stakeholderService.updateContributors(stakeholderId, emails), HttpStatus.OK);
    }

    @PostMapping("{id}/contributors")
    @PreAuthorize("hasAuthority('ADMIN')")// or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<Stakeholder> addContributor(@PathVariable("id") String stakeholderId, @RequestBody String email) {
        return new ResponseEntity<>(stakeholderService.addContributor(stakeholderId, email), HttpStatus.OK);
    }

    @DeleteMapping("{id}/contributors")
    @PreAuthorize("hasAuthority('ADMIN')")// or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<Stakeholder> removeContributor(@PathVariable("id") String stakeholderId, @RequestBody String email) {
        return new ResponseEntity<>(stakeholderService.removeContributor(stakeholderId, email), HttpStatus.OK);
    }

    @PatchMapping("{id}/managers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Stakeholder> updateManagers(@PathVariable("id") String stakeholderId, @RequestBody List<String> emails) {
        return new ResponseEntity<>(stakeholderService.updateManagers(stakeholderId, emails), HttpStatus.OK);
    }


    @PostMapping("{id}/managers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Stakeholder> addManager(@PathVariable("id") String stakeholderId, @RequestBody String email) {
        return new ResponseEntity<>(stakeholderService.addManager(stakeholderId, email), HttpStatus.OK);
    }

    @DeleteMapping("{id}/managers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Stakeholder> removeManager(@PathVariable("id") String stakeholderId, @RequestBody String email) {
        return new ResponseEntity<>(stakeholderService.removeManager(stakeholderId, email), HttpStatus.OK);
    }
}
