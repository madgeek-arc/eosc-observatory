package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.StakeholderMembers;
import eu.eosc.observatory.service.StakeholderService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("stakeholders")
public class StakeholderController {

    private static final Logger logger = LoggerFactory.getLogger(StakeholderController.class);

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
        return new ResponseEntity<>(stakeholderService.updateStakeholderAndUserPermissions(id, stakeholder), HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Stakeholder> delete(@PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(stakeholderService.delete(id), HttpStatus.OK);
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
    public ResponseEntity<Browsing<Stakeholder>> getStakeholders(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = GenericItemController.createFacetFilter(allRequestParams);
        Browsing<Stakeholder> stakeholders = stakeholderService.getAll(filter);
        return new ResponseEntity<>(stakeholders, HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Other methods       */
    /*---------------------------*/

    @GetMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderMember(#stakeholderId)")
    public ResponseEntity<StakeholderMembers> getMembers(@PathVariable("id") String stakeholderId) {
        return new ResponseEntity<>(stakeholderService.getMembers(stakeholderId), HttpStatus.OK);
    }

    @PatchMapping("{id}/contributors")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<Stakeholder> updateContributors(@PathVariable("id") String stakeholderId, @RequestBody List<String> userIds) {
        return new ResponseEntity<>(stakeholderService.updateContributors(stakeholderId, userIds), HttpStatus.OK);
    }

    @PostMapping("{id}/contributors")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<StakeholderMembers> addContributor(@PathVariable("id") String stakeholderId, @RequestBody String userId, @ApiIgnore Authentication authentication) {
        return new ResponseEntity<>(stakeholderService.addContributor(stakeholderId, userId), HttpStatus.OK);
    }

    @DeleteMapping("{id}/contributors/{userId}")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<StakeholderMembers> removeContributor(@PathVariable("id") String stakeholderId, @PathVariable("userId") String userId) {
        return new ResponseEntity<>(stakeholderService.removeContributor(stakeholderId, userId), HttpStatus.OK);
    }

    @PatchMapping("{id}/managers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Stakeholder> updateManagers(@PathVariable("id") String stakeholderId, @RequestBody List<String> emails) {
        return new ResponseEntity<>(stakeholderService.updateManagers(stakeholderId, emails), HttpStatus.OK);
    }


    @PostMapping("{id}/managers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StakeholderMembers> addManager(@PathVariable("id") String stakeholderId, @RequestBody String email) {
        return new ResponseEntity<>(stakeholderService.addManager(stakeholderId, email), HttpStatus.OK);
    }

    @DeleteMapping("{id}/managers/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<StakeholderMembers> removeManager(@PathVariable("id") String stakeholderId, @PathVariable("userId") String userId) {
        return new ResponseEntity<>(stakeholderService.removeManager(stakeholderId, userId), HttpStatus.OK);
    }

    @GetMapping("/countries")
    public ResponseEntity<List<String>> getCountryCodesByStakeholderType(@RequestParam("type") String stakeholderType) {
        return new ResponseEntity<>(getStakeholderCountryCodesByType(stakeholderType), HttpStatus.OK);
    }

    private List<String> getStakeholderCountryCodesByType(String stakeholderType) {
        Set<Stakeholder> countryStakeholders = stakeholderService.getWithFilter("type", stakeholderType);
        return countryStakeholders == null ? new ArrayList<>() : countryStakeholders
                .stream()
                .map(Stakeholder::getCountry)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }
}
