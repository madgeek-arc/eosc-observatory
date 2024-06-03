package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.GroupMembers;
import eu.eosc.observatory.service.CoordinatorService;
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
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("coordinators")
public class CoordinatorController {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatorController.class);

    private final CoordinatorService coordinatorService;
    private final UserService userService;

    public CoordinatorController(CoordinatorService coordinatorService,
                                 UserService userService) {
        this.coordinatorService = coordinatorService;
        this.userService = userService;
    }

    /*---------------------------*/
    /*        CRUD methods       */
    /*---------------------------*/

    @GetMapping("{id}")
    @PreAuthorize("isCoordinator(#id)")
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

    @Browse
    @GetMapping()
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Browsing<Coordinator>> getCoordinators(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = PagingUtils.createFacetFilter(allRequestParams);
        Browsing<Coordinator> coordinators = coordinatorService.getAll(filter);
        return new ResponseEntity<>(coordinators, HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Member methods      */
    /*---------------------------*/

    @GetMapping("{id}/users")
    @PreAuthorize("hasAuthority('ADMIN') or isCoordinatorMember(#coordinatorId)")
    public ResponseEntity<GroupMembers<User>> getUsers(@PathVariable("id") String coordinatorId) {
        return new ResponseEntity<>(coordinatorService.getGroupMembers(coordinatorId).map(userService::getUser), HttpStatus.OK);
    }

    @GetMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")// or isCoordinatorMember(#coordinatorId)")
    public ResponseEntity<GroupMembers<User>> getMembers(@PathVariable("id") String coordinatorId) {
        return new ResponseEntity<>(coordinatorService.getGroupMembers(coordinatorId).map(userService::getUser), HttpStatus.OK);
    }

    @PatchMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<?>> updateMembers(@PathVariable("id") String coordinatorId, @RequestBody Set<String> emails) {
        return new ResponseEntity<>(coordinatorService.updateMembers(coordinatorId, emails), HttpStatus.OK);
    }


    @PostMapping("{id}/members")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> addMember(@PathVariable("id") String coordinatorId, @RequestBody String email) {
        return new ResponseEntity<>(coordinatorService.addMember(coordinatorId, email), HttpStatus.OK);
    }

    @DeleteMapping("{id}/members/{memberId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Set<String>> removeMember(@PathVariable("id") String coordinatorId, @PathVariable("memberId") String memberId) {
        return new ResponseEntity<>(coordinatorService.removeMember(coordinatorId, memberId), HttpStatus.OK);
    }

    /*---------------------------*/
    /*       Other methods       */
    /*---------------------------*/


}

