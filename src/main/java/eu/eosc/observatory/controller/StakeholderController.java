package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.GroupMembers;
import eu.eosc.observatory.dto.StakeholderDTO;
import eu.eosc.observatory.mappers.StakeholderMapper;
import eu.eosc.observatory.service.StakeholderService;
import eu.eosc.observatory.service.UserService;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.utils.PagingUtils;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("stakeholders")
public class StakeholderController {

    private static final Logger logger = LoggerFactory.getLogger(StakeholderController.class);

    private final StakeholderService stakeholderService;
    private final StakeholderMapper stakeholderMapper;
    private final UserService userService;

    public StakeholderController(StakeholderService stakeholderService,
                                 StakeholderMapper stakeholderMapper,
                                 UserService userService) {
        this.stakeholderService = stakeholderService;
        this.stakeholderMapper = stakeholderMapper;
        this.userService = userService;
    }

    /*---------------------------*/
    /*        CRUD methods       */
    /*---------------------------*/

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderMember(#id) or isCoordinatorMemberOfStakeholder(#id)")
    public ResponseEntity<Stakeholder> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(stakeholderService.get(id), HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorMemberOfType(#dto.getType())")
    public ResponseEntity<Stakeholder> create(@RequestBody StakeholderDTO dto) {
        return new ResponseEntity<>(stakeholderService.add(stakeholderMapper.toStakeholder(dto)), HttpStatus.CREATED);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorMemberOfStakeholder(#id)")
    public ResponseEntity<Stakeholder> update(@PathVariable("id") String id, @RequestBody StakeholderDTO dto) {
        Stakeholder existing = stakeholderService.get(id);
        Stakeholder toUpdate = stakeholderMapper.toStakeholder(dto);
        toUpdate.setMembers(existing.getMembers());
        toUpdate.setAdmins(existing.getAdmins());
        return new ResponseEntity<>(stakeholderService.updateStakeholderAndUserPermissions(id, toUpdate), HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Stakeholder> delete(@PathVariable("id") String id) throws ResourceNotFoundException {
        return new ResponseEntity<>(stakeholderService.delete(id), HttpStatus.OK);
    }

    @Browse
    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorMemberOfType(#allRequestParams.get('type'))")
    public ResponseEntity<Browsing<Stakeholder>> getStakeholders(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = PagingUtils.createFacetFilter(allRequestParams);
        Browsing<Stakeholder> stakeholders = stakeholderService.getAll(filter);
        return new ResponseEntity<>(stakeholders, HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Other methods       */
    /*---------------------------*/

    @GetMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN') or isStakeholderMember(#stakeholderId) or isCoordinatorMemberOfStakeholder(#stakeholderId)")
    public ResponseEntity<GroupMembers<User>> getMembers(@PathVariable("id") String stakeholderId) {
        return new ResponseEntity<>(stakeholderService.getGroupMembers(stakeholderId).map(userService::getUser), HttpStatus.OK);
    }

    @PatchMapping("{id}/contributors")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Stakeholder> updateContributors(@PathVariable("id") String stakeholderId, @RequestBody Set<String> userIds) {
        return new ResponseEntity<>(stakeholderService.updateContributors(stakeholderId, userIds), HttpStatus.OK);
    }

    @PostMapping("{id}/contributors")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> addContributor(@PathVariable("id") String stakeholderId, @RequestBody String userId, @Parameter(hidden = true) Authentication authentication) {
        return new ResponseEntity<>(stakeholderService.addMember(stakeholderId, userId), HttpStatus.OK);
    }

    @DeleteMapping("{id}/contributors/{userId}")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorMemberOfStakeholder(#stakeholderId) or isStakeholderManager(#stakeholderId)")
    public ResponseEntity<Set<String>> removeContributor(@PathVariable("id") String stakeholderId, @PathVariable("userId") String userId) {
        return new ResponseEntity<>(stakeholderService.removeMember(stakeholderId, userId), HttpStatus.OK);
    }

    @PatchMapping("{id}/managers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Stakeholder> updateManagers(@PathVariable("id") String stakeholderId, @RequestBody Set<String> emails) {
        return new ResponseEntity<>(stakeholderService.updateManagers(stakeholderId, emails), HttpStatus.OK);
    }


    @PostMapping("{id}/managers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> addManager(@PathVariable("id") String stakeholderId, @RequestBody String email) {
        return new ResponseEntity<>(stakeholderService.addAdmin(stakeholderId, email), HttpStatus.OK);
    }

    @DeleteMapping("{id}/managers/{userId}")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorMemberOfStakeholder(#stakeholderId)")
    public ResponseEntity<Set<String>> removeManager(@PathVariable("id") String stakeholderId, @PathVariable("userId") String userId) {
        return new ResponseEntity<>(stakeholderService.removeAdmin(stakeholderId, userId), HttpStatus.OK);
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
